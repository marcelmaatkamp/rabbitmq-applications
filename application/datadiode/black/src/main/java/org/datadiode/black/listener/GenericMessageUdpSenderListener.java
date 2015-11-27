package org.datadiode.black.listener;

import com.rabbitmq.client.Channel;
import com.thoughtworks.xstream.XStream;
import org.compression.CompressionUtils;
import org.datadiode.model.message.ExchangeMessage;
import org.datadiode.service.RabbitMQService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.SerializationUtils;

/**
 * Created by marcelmaatkamp on 15/10/15.
 */
public class GenericMessageUdpSenderListener implements ChannelAwareMessageListener {
    static final Integer lock = new Integer(-1);
    private static final Logger log = LoggerFactory.getLogger(GenericMessageUdpSenderListener.class);
    @Autowired
    UnicastSendingMessageHandler unicastSendingMessageHandler;
    @Autowired
    RabbitMQService rabbitMQService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    XStream xStream;
    @Value(value = "${application.datadiode.black.udp.throttleInMs}")
    Integer throttleInMs;
    boolean compress;
    int maxBytes = 1450;

    @Autowired
    RabbitManagementTemplate rabbitManagementTemplate;

    public static int[][] chunkArray(int[] array, int chunkSize) {
        int numOfChunks = (int) Math.ceil((double) array.length / chunkSize);
        int[][] output = new int[numOfChunks][];

        for (int i = 0; i < numOfChunks; ++i) {
            int start = i * chunkSize;
            int length = Math.min(array.length - start, chunkSize);

            int[] temp = new int[length];
            System.arraycopy(array, start, temp, 0, length);
            output[i] = temp;
        }

        return output;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    /**
     * @param message
     * @param channel
     * @throws Exception
     */
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        // convert to exchange message

        // TODO: seperate thread
        ExchangeMessage exchangeMessage = rabbitMQService.getExchangeMessage(rabbitManagementTemplate, message);

        // log results
        if (log.isDebugEnabled()) {
            Object o = rabbitTemplate.getMessageConverter().fromMessage(message);
            if (o instanceof byte[]) {
                log.debug("exchangeMessage(" + exchangeMessage.getExchangeData() + "): routing(" + exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey() + "): " + new String((byte[]) o, "UTF-8"));

            } else {
                log.debug("exchangeMessage(" + exchangeMessage.getExchangeData() + "): routing(" + exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey() + "): " + o);
            }
        }

        // convert to generic message
        byte[] udpPacket = SerializationUtils.serialize(exchangeMessage);
        byte[] data = udpPacket;

        if (compress) {
            data = CompressionUtils.compress(udpPacket);
            if (log.isDebugEnabled()) {
                log.debug("udp: exchange(" + message.getMessageProperties().getReceivedExchange() + "): body(" + message.getBody().length + "), message(" + SerializationUtils.serialize(message).length + "),  exchange(" + udpPacket.length + "), compressed(" + data.length + ")");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("udp: exchange(" + message.getMessageProperties().getReceivedExchange() + "): body(" + message.getBody().length + "), message(" + SerializationUtils.serialize(message).length + "),  exchange(" + udpPacket.length + ")");
            }
        }

        if (data.length > maxBytes) {
            log.warn("too many bytes: " + data.length + ", max=" + maxBytes);
        }
        GenericMessage genericMessage = new GenericMessage<byte[]>(data);
        // send over udp

        // throttle thread
        try {
            synchronized (lock) {
                unicastSendingMessageHandler.handleMessageInternal(genericMessage);
                Thread.sleep(throttleInMs);
            }

        } catch (InterruptedException e) {
            log.error("Exception: ", e);
        }
    }


}
