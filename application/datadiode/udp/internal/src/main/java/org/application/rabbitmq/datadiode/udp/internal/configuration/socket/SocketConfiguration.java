package org.application.rabbitmq.datadiode.udp.internal.configuration.socket;

import com.google.common.primitives.Ints;
import org.application.rabbitmq.datadiode.configuration.xstream.XStreamConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

/**
 * Created by marcel on 06-12-15.
 */
@Configuration
@Import(XStreamConfiguration.class)

public class SocketConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SocketConfiguration.class);

    @Value( value = "${application.datadiode.udp.internal.port}")
    int port;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    DatagramChannel datagramChannel() throws IOException {
        DatagramChannel datagramChannel = DatagramChannel.open();
        return datagramChannel;
    }

    @Bean
    DatagramSocket socket() throws IOException {
        DatagramSocket socket = datagramChannel().socket();
        return socket;
    }

    @Bean
    SocketAddress socketAddress() throws IOException {
        SocketAddress address = new InetSocketAddress(port);
        socket().bind(address);
        return address;
    }

    @Bean
    ByteBuffer buffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(65507);
        return buffer;
    }

    public void receive() throws IOException {
        byte[] message = new byte[8192];
        MessageProperties messageProperties = new MessageProperties();
        while (true) {
            DatagramPacket packet = new DatagramPacket(message, message.length);
            socket().receive(packet);
            byte[] m = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
            // log.info("received("+packet.getLength()+")");

            rabbitTemplate.send("udp", null, new Message(m,messageProperties));
        }

    }


}
