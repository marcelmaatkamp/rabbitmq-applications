package org.application.rabbitmq.datadiode.model.message;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.amqp.core.Message;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by marcelmaatkamp on 27/10/15.
 */
public class ExchangeMessage implements Serializable {

    public UUID getUuid() {
        return uuid;
    }

    UUID uuid = UUID.randomUUID();
    Message message;
    String exchangeData;

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    byte[] digest;

    public byte[] getDigest() {
        return digest;
    }

    public ExchangeMessage(Message message, String exchangeData) {
        this.message = message;
        this.exchangeData = exchangeData;
        this.digest = digest;
    }

    public String getExchangeData() {
        return exchangeData;
    }

    public Message getMessage() {
        return message;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
