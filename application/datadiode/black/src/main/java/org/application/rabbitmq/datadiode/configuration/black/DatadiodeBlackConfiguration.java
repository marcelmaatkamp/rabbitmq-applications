package org.application.rabbitmq.datadiode.configuration.black;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;

import javax.annotation.PostConstruct;

/**
 * Created by marcel on 28-11-15.
 */
public class DatadiodeBlackConfiguration {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @PostConstruct
    void init() {
        rabbitAdmin.declareBinding(new Binding("udp", Binding.DestinationType.EXCHANGE, "cutter", "", null));


    }
}
