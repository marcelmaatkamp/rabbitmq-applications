package org.sensor.temperature.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.config.EnableIntegration;

import java.io.IOException;

@SpringBootApplication
@EnableIntegration
@EnableAutoConfiguration(exclude={RabbitAutoConfiguration.class})
public class TemperatureSensorReceiverStarter {
    private static final Logger log = LoggerFactory.getLogger(TemperatureSensorReceiverStarter.class);

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext configurableApplicationContext = new SpringApplicationBuilder(TemperatureSensorReceiverStarter.class).web(false).run(args);
    }
}