package org.datadiode.red.configuration.rabbitmq;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.datadiode.red.listener.SensorEventListener;
import org.datadiode.service.RabbitMQService;
import org.datadiode.service.RabbitMQServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by marcel on 23-09-15.
 */
@Configuration
public class RabbitmqConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RabbitmqConfiguration.class);

    @Autowired
    Environment environment;


    @Bean
    public MessageConverter jsonMessageConverter() {
        JsonMessageConverter jsonMessageConverter = new JsonMessageConverter();
        jsonMessageConverter.setJsonObjectMapper(objectMapper());
        jsonMessageConverter.setClassMapper(defaultClassMapper());
        return jsonMessageConverter;
    }

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper
                .configure(
                        DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
        jsonObjectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        return jsonObjectMapper;
    }

    @Bean
    public DefaultClassMapper defaultClassMapper() {
        DefaultClassMapper defaultClassMapper = new DefaultClassMapper();
        return defaultClassMapper;
    }


    @Bean
    RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        // rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }
    @Bean
    RabbitManagementTemplate rabbitManagementTemplate() {
        RabbitManagementTemplate rabbitManagementTemplate = new RabbitManagementTemplate(
                "http://" + environment.getProperty("spring.datadiode.rabbitmq.host") + ":" + environment.getProperty("spring.datadiode.rabbitmq.port.management", Integer.class) + "/api/",
                environment.getProperty("spring.datadiode.rabbitmq.username"),
                environment.getProperty("spring.datadiode.rabbitmq.password")
        );
        return rabbitManagementTemplate;
    }

    @Bean
    Map<String, Exchange> declaredExchanges() {
        Map<String, Exchange> declaredExchanges = new HashMap<>();
        for(Exchange exchange : rabbitManagementTemplate().getExchanges()) {
            declaredExchanges.put(exchange.getName(), exchange);
        }
        return  declaredExchanges;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL); // http://docs.spring.io/spring-amqp/docs/1.3.1.RELEASE/reference/html/amqp.html#automatic-declaration
        connectionFactory.setHost(environment.getProperty("spring.datadiode.rabbitmq.host"));
        connectionFactory.setPort(environment.getProperty("spring.datadiode.rabbitmq.port", Integer.class));
        connectionFactory.setUsername(environment.getProperty("spring.datadiode.rabbitmq.username"));
        connectionFactory.setPassword(environment.getProperty("spring.datadiode.rabbitmq.password"));
        connectionFactory.createConnection();
        log.info("rabbitmq(" + connectionFactory.getHost() + ":" + connectionFactory.getPort() + ").channelCacheSize(" + connectionFactory.getChannelCacheSize() + ")");
        return connectionFactory;
    }


    @Bean
    Exchange sensorExchange() {
        Exchange exchange = new FanoutExchange("sensor");
        return exchange;
    }

    @Bean
    Queue sensorQueue() {
        Queue queue = new Queue("sensor");
        return queue;
    }

    @Bean
    BindingBuilder.GenericArgumentsConfigurer sensorQueueBinding() {
        BindingBuilder.GenericArgumentsConfigurer destinationConfigurer = BindingBuilder.bind(sensorQueue()).to(sensorExchange()).with("");
        rabbitAdmin().declareBinding(new Binding(sensorQueue().getName(), Binding.DestinationType.QUEUE, sensorExchange().getName(), "", null));
        return destinationConfigurer;
    }

    @Bean
    SensorEventListener sensorEventListener() {
        SensorEventListener sensorEventListenerer = new SensorEventListener();
        return sensorEventListenerer;
    }

    @Bean
    SimpleMessageListenerContainer sensorListenerContainer() {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(sensorEventListener());
        simpleMessageListenerContainer.setQueueNames(sensorQueue().getName());
        simpleMessageListenerContainer.setMessageListener(messageListenerAdapter);
        simpleMessageListenerContainer.start();
        return simpleMessageListenerContainer;
    }


    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }

}
