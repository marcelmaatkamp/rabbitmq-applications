package org.application.rabbitmq.datadiode.udp.external.listener;

import com.google.common.util.concurrent.RateLimiter;
import com.rabbitmq.client.Channel;
import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.udp.external.service.DatagramSocketService;
import org.compression.CompressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
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

    // @Autowired
    // UnicastSendingMessageHandler unicastSendingMessageHandler;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    XStream xStream;

    @Value(value = "${application.datadiode.udp.external.throttleInMs}")
    Integer throttleInMs;

    boolean compress;

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    @Autowired
    DatagramSocketService datagramSocketService;

    final RateLimiter rateLimiter = RateLimiter.create(1000000.0); // rate = 1M

    /**
     * @param message
     * @param channel
     * @throws Exception
     */
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {

        // convert to generic message
        byte[] data = message.getBody();

        if(log.isDebugEnabled()) {
            log.debug("[" + data.length + "]: " + new String(data, "UTF-8"));
        }

        if (compress) {
            data = CompressionUtils.compress(data);
            if (log.isDebugEnabled()) {
                log.debug("udp: exchange(" + message.getMessageProperties().getReceivedExchange() + "): body(" + message.getBody().length + "),  compressed(" + data.length + "), ratio("+Math.round((100.0/data.length)*message.getBody().length)+"%)");
            }
        }
        // else {
        //    if (log.isDebugEnabled()) {
        //        log.debug("udp: exchange(" + message.getMessageProperties().getReceivedExchange() + "): body(" + message.getBody().length + ")");
        //    }
        //}

        // if (data.length > maxBytes) {
        //     log.warn("too many bytes: " + data.length + ", max=" + maxBytes);
        // }


        // GenericMessage genericMessage = new GenericMessage<byte[]>(data);
        // unicastSendingMessageHandler.handleMessageInternal(genericMessage);
        // Thread.sleep(throttleInMs);
        
        rateLimiter.acquire(1);
        datagramSocketService.send(data);
    }


}
