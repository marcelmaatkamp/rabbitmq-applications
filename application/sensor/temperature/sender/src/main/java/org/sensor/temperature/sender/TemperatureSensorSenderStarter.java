package org.sensor.temperature.sender;

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
public class TemperatureSensorSenderStarter {
    private static final Logger log = LoggerFactory.getLogger(TemperatureSensorSenderStarter.class);

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext configurableApplicationContext = new SpringApplicationBuilder(TemperatureSensorSenderStarter.class).web(false).run(args);
    }
}
