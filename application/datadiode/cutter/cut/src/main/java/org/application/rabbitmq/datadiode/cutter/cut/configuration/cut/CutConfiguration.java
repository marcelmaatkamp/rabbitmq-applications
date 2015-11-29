package org.application.rabbitmq.datadiode.cutter.cut.configuration.cut;

import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.stream.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Configuration
public class CutConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CutConfiguration.class);

    @Value(value = "${application.datadiode.cutter.size}")
    int maxMessageSize;
    @Value(value = "${application.datadiode.cutter.redundancyFactor}")
    int redundancyFactor;
    @Autowired
    XStream xStream;
    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

    @Autowired
    Environment environment;

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
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        return rabbitAdmin;
    }
    // TODO: naamgeving in application.properties

    @Bean
    org.springframework.amqp.core.Exchange cutterExchange() {
        org.springframework.amqp.core.Exchange exchange = new FanoutExchange(environment.getProperty("application.datadiode.cutter.cutted.exchange"));
        rabbitAdmin().declareExchange(exchange);
        return exchange;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${application.datadiode.cutter.queue}", durable = "true"),
                    exchange = @Exchange(value = "${application.datadiode.cutter.exchange}", durable = "true", autoDelete = "true", type = "fanout"))
    )
    void cut(Message message) {
        List<Message> messages = StreamUtils.cut(message, maxMessageSize, redundancyFactor);

        log.info("cutting message(" + message.getBody().length + ") into " + messages.size() + " messages of " + maxMessageSize + " bytes..");
        for (Message m : messages) {
            rabbitTemplate.convertAndSend(cutterExchange().getName(), null, m);
        }
    }

/*
    @Scheduled(fixedRate = 25)
    public void sendMessage() throws MalformedURLException {
        int length = 20000;
        rabbitTemplate.send("cut", null, new Message(RandomUtils.nextBytes(length), new MessageProperties()));
    }
*/

}
