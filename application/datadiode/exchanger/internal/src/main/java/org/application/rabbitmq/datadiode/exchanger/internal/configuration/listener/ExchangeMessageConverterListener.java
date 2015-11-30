package org.application.rabbitmq.datadiode.exchanger.internal.configuration.listener;

import com.rabbitmq.client.Channel;
import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.model.message.ExchangeMessage;
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

import javax.annotation.Resource;
import java.util.Map;

/**
 * Created by marcelmaatkamp on 15/10/15.
 */
@Resource
public class ExchangeMessageConverterListener implements ChannelAwareMessageListener {

    // TODO: exchange::exchange -> exchanged

    static final Integer lock = new Integer(-1);
    private static final Logger log = LoggerFactory.getLogger(ExchangeMessageConverterListener.class);


    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    XStream xStream;

    @Autowired
    RabbitManagementTemplate rabbitManagementTemplate;

    @Autowired
    Exchange exchangeExchange;

    @Autowired
    RabbitMQService rabbitMQService;

    /**
     * @param message
     * @param channel
     * @throws Exception
     */
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        rabbitMQService.sendExchangeMessage((ExchangeMessage)rabbitTemplate.getMessageConverter().fromMessage(message));
    }

    @Resource
    Map<String, String> declaredExchanges;

}
