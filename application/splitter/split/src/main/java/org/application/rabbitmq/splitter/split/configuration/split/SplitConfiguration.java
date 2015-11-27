package org.application.rabbitmq.splitter.split.configuration.split;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.RandomUtils;
import org.application.rabbitmq.stream.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Configuration
@EnableScheduling
public class SplitConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SplitConfiguration.class);

    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

    @Value(value="${application.stream.cutter.size}")
    int maxMessageSize;

    @Value(value="${application.stream.cutter.redundancyFactor}")
    int redundancyFactor;

    @Bean
    MessageDigest messageDigest() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest;
    }

    @Bean
    StreamUtils streamUtils() {
        StreamUtils streamUtils = new StreamUtils();
        return streamUtils;
    }

    @Bean
    org.springframework.amqp.core.Exchange cutterExchange() {
        org.springframework.amqp.core.Exchange exchange = new FanoutExchange("cutter");
        return exchange;
    }

    @Autowired
    XStream xStream;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "cut", durable = "true"),
                    exchange = @Exchange(value = "cut", durable = "true", autoDelete = "true"))
    )
    void cut(Message message) {
        List<Message> messages = StreamUtils.cut(message, maxMessageSize, redundancyFactor);

        log.info("cutting message("+message.getBody().length+") into " + messages.size() + " messages of " + maxMessageSize +" bytes..");
        for(Message m : messages) {
            rabbitTemplate.convertAndSend(cutterExchange().getName(), null, m);
        }
    }



    @Scheduled(fixedRate = 25)
    public void sendMessage() throws MalformedURLException {
        int length = 20000;
        rabbitTemplate.send("cut", null, new Message(RandomUtils.nextBytes(length), new MessageProperties()));
    }


}
