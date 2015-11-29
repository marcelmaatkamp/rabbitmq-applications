package org.application.rabbitmq.datadiode.exchanger.internal.configuration.internal;

import org.application.rabbitmq.datadiode.service.RabbitMQService;
import org.application.rabbitmq.datadiode.service.RabbitMQServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by marcel on 28-11-15.
 */
@Configuration
public class ExchangerInternalConfiguration {


    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }
}
