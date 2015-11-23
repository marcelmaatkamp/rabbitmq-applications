package org.application.rabbitmq.search.consumer.configuration.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by marcelmaatkamp on 23/11/15.
 */
@Configuration
public class SearchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SearchConfiguration.class);

    private final AtomicInteger counter = new AtomicInteger();

    @Autowired
    ConnectionFactory connectionFactory;

    @Bean
    public SimpleRabbitListenerContainerFactory myRabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }

    @RabbitListener(
            containerFactory="myRabbitListenerContainerFactory",
            bindings = @QueueBinding(
            value = @Queue(value = "url", durable = "true"),
            exchange = @Exchange(value = "url"))
    )
    public void process(String message) {
        log.info("message("+message+")");
    }
}
