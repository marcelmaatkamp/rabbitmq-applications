package org.application.rabbitmq.datadiode.exchanger.internal.configuration.rabbitmq;

import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.configuration.xstream.XStreamConfiguration;
import org.application.rabbitmq.datadiode.service.RabbitMQService;
import org.application.rabbitmq.datadiode.service.RabbitMQServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by marcel on 29-11-15.
 */
@Configuration
@Import(XStreamConfiguration.class)
public class RabbitMQConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfiguration.class);

    @Autowired
    XStream xStream;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    Environment environment;

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        return rabbitAdmin;
    }

    @Bean
    RabbitManagementTemplate rabbitManagementTemplate() {
        RabbitManagementTemplate rabbitManagementTemplate = new RabbitManagementTemplate(
                "http://" + environment.getProperty("spring.rabbitmq.host") + ":" + environment.getProperty("spring.rabbitmq.management.port", Integer.class) + "/api/",
                environment.getProperty("spring.rabbitmq.username"),
                environment.getProperty("spring.rabbitmq.password")
        );
        return rabbitManagementTemplate;
    }

    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }

    @Bean
    Map<String, String> declaredExchanges() {
        Map<String, String> declaredExchanges = new HashMap();
        for (Exchange exchange : rabbitManagementTemplate().getExchanges()) {
            declaredExchanges.put(exchange.getName(), xStream.toXML(exchange));
        }
        return declaredExchanges;
    }
}
