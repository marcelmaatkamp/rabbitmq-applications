package org.application.rabbitmq.search.producer.configuration.search;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
@EnableScheduling
public class SearchConfiguration {

    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

    private final AtomicInteger counter = new AtomicInteger();

    @Scheduled(fixedRate = 3000)
    public void sendMessage() throws MalformedURLException {
        rabbitTemplate.convertAndSend("url", new URL("http://www.nu.nl"));
    }
}
