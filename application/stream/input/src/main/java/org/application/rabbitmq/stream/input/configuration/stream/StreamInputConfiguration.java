package org.application.rabbitmq.stream.input.configuration.stream;

import org.apache.commons.lang3.RandomUtils;
import org.application.rabbitmq.stream.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Configuration
@EnableScheduling
public class StreamInputConfiguration {
    private static final Logger log = LoggerFactory.getLogger(StreamInputConfiguration.class);

    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

    int maxMessageSize = 700;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "cut", durable = "true"),
                    exchange = @Exchange(value = "cut"))
    )
    void cut(Message message) {
        List<Message> messages = StreamUtils.cut(message, maxMessageSize);
        log.info("message("+message.getBody().length+") into " + messages.size());
        for(Message m : messages) {
            rabbitTemplate.convertAndSend("cutter", null, m);
        }
    }



    @Scheduled(fixedRate = 1000)
    public void sendMessage() throws MalformedURLException {
        int length = 90000;

        byte[] randomBytes = RandomUtils.nextBytes(length);
        Message message = new Message(randomBytes, new MessageProperties());

        rabbitTemplate.send("cut", null, message);
    }


}
