package org.application.rabbitmq.datadiode.udp.internal.service;

import com.thoughtworks.xstream.XStream;
import org.compression.CompressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    MessageProperties messageProperties = new MessageProperties();

    public void udpMessage(Message message) throws IOException, DataFormatException {

        if(log.isTraceEnabled()) {
            log.trace(xStream.toXML(message));
        }

        // from udp
        byte[] udpPacket = (byte[]) message.getPayload();

        org.springframework.amqp.core.Message messageToUdp = new org.springframework.amqp.core.Message(
                (compress ?  CompressionUtils.decompress(udpPacket) : udpPacket),
                messageProperties
        );

        rabbitTemplate.send("udp",null,messageToUdp);
    }

    @Override
    public void setCompress(boolean compress) {
        this.compress = compress;
    }


}
