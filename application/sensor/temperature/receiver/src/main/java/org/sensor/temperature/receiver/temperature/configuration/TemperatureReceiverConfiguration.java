package org.sensor.temperature.receiver.temperature.configuration;

import org.event.configuration.xstream.XStreamConfiguration;
import org.sensor.temperature.receiver.temperature.listener.TemperatureSensorEventListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by marcel on 27-11-15.
 */
@Configuration
@Import(XStreamConfiguration.class)
public class TemperatureReceiverConfiguration {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
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
    TemperatureSensorEventListener sensorEventListener() {
        TemperatureSensorEventListener sensorEventListenerer = new TemperatureSensorEventListener();
        return sensorEventListenerer;
    }

    @Bean
    SimpleMessageListenerContainer sensorListenerContainer() {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(sensorEventListener());
        simpleMessageListenerContainer.setQueueNames(sensorQueue().getName());
        simpleMessageListenerContainer.setMessageListener(messageListenerAdapter);
        simpleMessageListenerContainer.start();
        return simpleMessageListenerContainer;
    }
}
