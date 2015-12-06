package org.application.rabbitmq.datadiode;

import org.application.rabbitmq.datadiode.udp.internal.configuration.socket.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.config.EnableIntegration;

import java.io.IOException;

@SpringBootApplication
// @EnableIntegration
// @ImportResource("integration.xml")
public class DatadiodeRedStarter {
    private static final Logger log = LoggerFactory.getLogger(DatadiodeRedStarter.class);

    @Autowired
    public static void main(String[] args) throws IOException, InterruptedException {
        ConfigurableApplicationContext configurableApplicationContext = new SpringApplicationBuilder(DatadiodeRedStarter.class).run(args);
        configurableApplicationContext.start();

        SocketConfiguration socketConfiguration = configurableApplicationContext.getBean(SocketConfiguration.class);
        socketConfiguration.receive();
    }
}
