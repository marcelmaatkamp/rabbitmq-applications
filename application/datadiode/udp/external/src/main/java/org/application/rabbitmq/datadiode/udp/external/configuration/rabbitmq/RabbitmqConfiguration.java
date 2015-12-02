package org.application.rabbitmq.datadiode.udp.external.configuration.rabbitmq;


import org.application.rabbitmq.datadiode.configuration.xstream.XStreamConfiguration;
import org.application.rabbitmq.datadiode.udp.external.listener.GenericMessageUdpSenderListener;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

/**
 * Created by marcel on 23-09-15.
 */
@Configuration
@Import(XStreamConfiguration.class)
public class RabbitMQConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfiguration.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public DefaultClassMapper defaultClassMapper() {
        DefaultClassMapper defaultClassMapper = new DefaultClassMapper();
        return defaultClassMapper;
    }

    @Bean
    public JsonMessageConverter jsonMessageConverter() {
        JsonMessageConverter jsonMessageConverter = new JsonMessageConverter();
        jsonMessageConverter.setJsonObjectMapper(objectMapper());
        jsonMessageConverter.setClassMapper(defaultClassMapper());
        DefaultClassMapper defaultClassMapper = defaultClassMapper();
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

    @PostConstruct
    void init() {
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
    }


    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        return rabbitAdmin;
    }


    @Bean
    Exchange udpEchange() {
        Exchange exchange = new FanoutExchange(environment.getProperty("application.datadiode.udp.external.exchange", String.class));
        rabbitAdmin().declareExchange(exchange);
        return exchange;
    }
    @Bean
    Queue udpQueue() {
        Queue queue = new Queue(environment.getProperty("application.datadiode.udp.external.queue", String.class));
        rabbitAdmin().declareQueue(queue);
        rabbitAdmin().declareBinding(new Binding(queue.getName(), Binding.DestinationType.QUEUE, udpEchange().getName(),"",null));
        return queue;
    }

    @Bean
    SimpleMessageListenerContainer simpleMessageListenerContainer() {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        simpleMessageListenerContainer.setQueueNames(udpQueue().getName());
        simpleMessageListenerContainer.setMessageListener(new MessageListenerAdapter(genericMessageUdpSenderListener()));
        simpleMessageListenerContainer.setConcurrentConsumers(environment.getProperty("application.datadiode.udp.external.concurrentConsumers", Integer.class));

        simpleMessageListenerContainer.start();
        return  simpleMessageListenerContainer;
    }


    @Autowired
    Environment environment;

    @Bean
    GenericMessageUdpSenderListener genericMessageUdpSenderListener() {
        GenericMessageUdpSenderListener genericMessageUdpSenderListener = new GenericMessageUdpSenderListener();
        genericMessageUdpSenderListener.setCompress(environment.getProperty("application.datadiode.udp.external.compress", Boolean.class));
        return genericMessageUdpSenderListener;
    }

}
