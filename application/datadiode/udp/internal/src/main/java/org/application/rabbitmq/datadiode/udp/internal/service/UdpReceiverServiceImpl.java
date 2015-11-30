package org.application.rabbitmq.datadiode.udp.internal.service;

import com.thoughtworks.xstream.XStream;
import org.compression.CompressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * Created by marcel on 07-10-15.
 */
@Service
public class UdpReceiverServiceImpl implements UdpReceiverService {
    private static final Logger log = LoggerFactory.getLogger(UdpReceiverServiceImpl.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    XStream xStream;

    boolean compress;

    @Autowired
    Environment environment;

    public void udpMessage(Message message) throws IOException, DataFormatException {

        if(log.isDebugEnabled()) {
            log.debug(xStream.toXML(message));
        }

        // from udp
        byte[] udpPacket = (byte[]) message.getPayload();
        byte[] data = udpPacket;

        if (compress) {
            data = CompressionUtils.decompress(udpPacket);
        }
        org.springframework.amqp.core.Message udpMessage =
                (org.springframework.amqp.core.Message) SerializationUtils.deserialize(
                        (byte[]) data);
/**
        ExchangeMessage exchangeMessage =
                (ExchangeMessage) SerializationUtils.deserialize(
                        (byte[]) data);

        if (log.isDebugEnabled()) {
            Object o = rabbitTemplate.getMessageConverter().fromMessage(exchangeMessage.getMessage());
            if (o instanceof byte[]) {
                log.debug("exchangeMessage(" + exchangeMessage.getExchangeData() + "): routing(" + exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey() + "): " + new String((byte[]) o, "UTF-8"));
            } else {
                log.debug("exchangeMessage(" + exchangeMessage.getExchangeData() + "): routing(" + exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey() + "): " + o);
            }
        }
    rabbitMQService.sendExchangeMessage(exchange().getName());

 */

        rabbitTemplate.send(udpMessage.getMessageProperties().getReceivedExchange(),udpMessage.getMessageProperties().getReceivedRoutingKey(),udpMessage);
    }

    @Override
    public void setCompress(boolean compress) {
        this.compress = compress;
    }


}
