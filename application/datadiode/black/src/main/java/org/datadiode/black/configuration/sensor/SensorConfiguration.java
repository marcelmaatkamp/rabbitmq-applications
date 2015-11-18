package org.datadiode.black.configuration.sensor;

import com.thoughtworks.xstream.XStream;
import org.bouncycastle.util.encoders.Base64;
import org.datadiode.model.event.GeoLocation;
import org.datadiode.model.event.sensor.Sensor;
import org.datadiode.model.event.sensor.SensorEvent;
import org.datadiode.model.event.sensor.temperature.TemperatureSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

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
public class SensorConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SensorConfiguration.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    Exchange sensorExchange;

    @Autowired
    Exchange encryptExchange;

    @Bean
    GeoLocation geoLocationAmsterdam() {
        return new GeoLocation(52.379189, 4.899431);
    }

    @Autowired
    XStream xStream;

    @Bean
    Sensor sensor() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException {
        TemperatureSensor temperatureSensor = new TemperatureSensor("temperature", 1, Base64.toBase64String(new String("marcel").getBytes()), geoLocationAmsterdam());
        return temperatureSensor;
    }

    AtomicInteger atomicInteger = new AtomicInteger(1);
    @Scheduled(fixedDelayString = "${application.datadiode.black.sensor.interval}")
    void sendSensorMessages() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SensorEvent sensorEvent = sensor().generateEvent(atomicInteger.getAndIncrement());
        if(log.isDebugEnabled()) {
            log.debug("sensor: " + xStream.toXML(sensorEvent));
        }
        rabbitTemplate.convertAndSend(sensorExchange.getName(), sensorEvent.getSensor().getTargetid(), sensorEvent);
    }
}
