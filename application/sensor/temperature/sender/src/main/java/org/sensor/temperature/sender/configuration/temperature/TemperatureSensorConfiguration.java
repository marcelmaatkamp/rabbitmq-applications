package org.sensor.temperature.sender.configuration.temperature;

import com.thoughtworks.xstream.XStream;
import org.event.configuration.xstream.XStreamConfiguration;
import org.event.model.GeoLocation;
import org.event.model.sensor.SensorEvent;
import org.event.model.sensor.temperature.TemperatureSensor;
import org.event.model.sensor.temperature.TemperatureSensorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Base64Utils;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by marcelmaatkamp on 15/10/15.
 */
@Configuration
@Import(XStreamConfiguration.class)
@EnableScheduling
public class TemperatureSensorConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TemperatureSensorConfiguration.class);

    @Autowired
    XStream xStream;

    @Autowired
    RabbitTemplate rabbitTemplate;

    AtomicInteger atomicInteger = new AtomicInteger(1);

    @Bean
    GeoLocation geoLocationAmsterdam() {
        return new GeoLocation(52.379189, 4.899431);
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    Exchange sensorExchange() {
        Exchange exchange = new FanoutExchange("sensor");
        rabbitAdmin().declareExchange(exchange);
        return exchange;
    }

    @Bean
    Exchange encryptedSensorExchange() {
        Exchange exchange = new FanoutExchange("encrypt");
        rabbitAdmin().declareExchange(exchange);
        return exchange;
    }

    @PostConstruct
    void init() {
        xStream.alias("temperatureSensor", TemperatureSensor.class);
        xStream.alias("temperatureSensorEvent", TemperatureSensorEvent.class);
    }

    @Bean
    TemperatureSensor temperatureSensor() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException {
        TemperatureSensor temperatureSensor = new TemperatureSensor("temperature", 1, Base64Utils.encodeToString(new String("marcel").getBytes()), geoLocationAmsterdam());
        return temperatureSensor;
    }

    @Scheduled(fixedDelayString = "${application.sensor.temperature.interval}")
    void sendSensorMessages() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SensorEvent sensorEvent = temperatureSensor().generateEvent(atomicInteger.getAndIncrement());
        if (log.isDebugEnabled()) {
            log.debug("sensor: " + xStream.toXML(sensorEvent));
        }
        rabbitTemplate.convertAndSend(sensorExchange().getName(), sensorEvent.getSensor().getTargetid(), sensorEvent);
    }
}
