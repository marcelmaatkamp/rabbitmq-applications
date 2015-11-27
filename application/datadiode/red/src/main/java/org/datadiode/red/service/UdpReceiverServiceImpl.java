package org.datadiode.red.service;

import com.thoughtworks.xstream.XStream;
import org.compression.CompressionUtils;
import org.datadiode.model.message.ExchangeMessage;
import org.datadiode.service.RabbitMQService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Created by marcel on 07-10-15.
 */
@Service
public class UdpReceiverServiceImpl implements UdpReceiverService {
    private static final Logger log = LoggerFactory.getLogger(UdpReceiverServiceImpl.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Resource
    Map<String, Exchange> declaredExchanges;

    @Autowired
    RabbitManagementTemplate rabbitManagementTemplate;

    @Autowired
    XStream xStream;

    @Autowired
    RabbitMQService rabbitMQService;

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    boolean compress;

    public void udpMessage(Message message) throws IOException, DataFormatException {

        // from udp
        byte[] udpPacket = (byte[]) message.getPayload();
        byte[] data = udpPacket;

        if(compress) {
            data = CompressionUtils.decompress(udpPacket);
        }

        ExchangeMessage exchangeMessage =
                (ExchangeMessage) SerializationUtils.deserialize(
                        (byte[]) data);

        if(log.isDebugEnabled()) {
            Object o = rabbitTemplate.getMessageConverter().fromMessage(exchangeMessage.getMessage());
            if(o instanceof byte[]) {
                log.debug("exchangeMessage(" + exchangeMessage.getExchangeData() + "): routing("+exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey()+"): " + new String((byte[])o, "UTF-8"));
            } else {
                log.debug("exchangeMessage(" + exchangeMessage.getExchangeData() + "): routing("+exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey()+"): " + o);
            }
        }

       rabbitMQService.sendExchangeMessage(exchangeMessage);
    }


}
