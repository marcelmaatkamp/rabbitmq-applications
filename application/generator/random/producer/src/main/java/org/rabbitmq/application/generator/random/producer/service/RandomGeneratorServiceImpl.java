package org.rabbitmq.application.generator.random.producer.service;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by marcel on 08-12-15.
 */
@Service
public class RandomGeneratorServiceImpl implements RandomGeneratorService {

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    private int size;
    private int count;

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    private String exchangeName;

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void generateRandomMessages() {
        byte[] data = RandomUtils.nextBytes(size);

        for (int i = 0; i < count; i++) {
            rabbitTemplate.send(exchangeName, null, new Message(data, new MessageProperties()));
        }
    }
}
