package org.application.rabbitmq.datadiode.exchanger.external.listener;

import com.rabbitmq.client.Channel;
import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.service.RabbitMQService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;

/**
 * Created by marcelmaatkamp on 15/10/15.
 */
public class ExchangeMessageConverterListener implements ChannelAwareMessageListener {
    private static final Logger log = LoggerFactory.getLogger(ExchangeMessageConverterListener.class);

    @Autowired
    RabbitMQService rabbitMQService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    XStream xStream;

    boolean compress;

    @Autowired
    RabbitManagementTemplate rabbitManagementTemplate;

    @Autowired
    Exchange exchangeExchange;

    /**
     * @param message
     * @param channel
     * @throws Exception
     */
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {

        // exchangeMessage
        rabbitTemplate.convertAndSend(exchangeExchange.getName(),null, rabbitMQService.getExchangeMessage(rabbitManagementTemplate, message));
    }


}
