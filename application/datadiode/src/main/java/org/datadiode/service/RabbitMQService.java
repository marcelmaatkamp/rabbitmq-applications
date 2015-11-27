package org.datadiode.service;

import org.datadiode.model.message.ExchangeMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;

/**
 * Created by marcelmaatkamp on 29/10/15.
 */
public interface RabbitMQService {
    public ExchangeMessage getExchangeMessage(RabbitManagementTemplate rabbitManagementTemplate, Message message);

    public void sendExchangeMessage(ExchangeMessage exchangeMessage);
}
