
package org.application.rabbitmq.datadiode.udp.internal;

import org.application.rabbitmq.datadiode.udp.internal.configuration.socket.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.config.EnableIntegration;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

@SpringBootApplication
// @EnableIntegration
// @ImportResource("integration.xml")
public class UdpInternalStarter {
    private static final Logger log = LoggerFactory.getLogger(UdpInternalStarter.class);

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext configurableApplicationContext = new SpringApplicationBuilder(UdpInternalStarter.class).web(false).run(args);
        configurableApplicationContext.start();

        // SocketConfiguration socketConfiguration = configurableApplicationContext.getBean(SocketConfiguration.class);
        // socketConfiguration.receive();
    }
}
