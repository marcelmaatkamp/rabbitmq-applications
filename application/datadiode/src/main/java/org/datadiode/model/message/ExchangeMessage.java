package org.datadiode.model.message;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.amqp.core.Message;

import java.io.Serializable;

/**
 * Created by marcelmaatkamp on 27/10/15.
 */
public class ExchangeMessage implements Serializable {

    Message message;
    String exchangeData;

    public ExchangeMessage(Message message, String exchangeData) {
        this.message = message;
        this.exchangeData = exchangeData;
    }

    public String getExchangeData() {
        return exchangeData;
    }

    public Message getMessage() {
        return message;
    }


    public String toString() {
        return  ReflectionToStringBuilder.toString(this);
    }

}
