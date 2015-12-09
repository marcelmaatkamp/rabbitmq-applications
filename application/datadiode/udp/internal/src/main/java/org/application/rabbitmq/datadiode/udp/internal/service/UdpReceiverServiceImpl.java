package org.application.rabbitmq.datadiode.udp.internal.service;

import com.google.common.primitives.Ints;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.thoughtworks.xstream.XStream;
import org.compression.CompressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;

/**
 * Created by marcel on 07-10-15.
 */
@Service
public class UdpReceiverServiceImpl implements UdpReceiverService {
    private static final Logger log = LoggerFactory.getLogger(UdpReceiverServiceImpl.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    XStream xStream;

    boolean compress;

    @Autowired
    Environment environment;

    MessageProperties messageProperties = new MessageProperties();

    static int serverPort = 9999;
    static int packetSize = 8192;

    static byte[] b = new byte[packetSize];

    ConnectionFactory factory;
    Connection conn;
    Channel channel;

    public void start() throws IOException, TimeoutException {
        channel = rabbitTemplate.getConnectionFactory().createConnection().createChannel(false);

        DatagramChannel channel = DatagramChannel.open();
        DatagramSocket socket = channel.socket();
        socket.setReceiveBufferSize(8192 * 128); // THIS!

        SocketAddress address = new InetSocketAddress(serverPort);
        socket.bind(address);

        byte[] message = new byte[packetSize];

        log.info("receiving udp packets on port " + serverPort);
        while (true) {
            DatagramPacket packet = new DatagramPacket(message, message.length);
            socket.receive(packet);
            byte[] m = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
            this.channel.basicPublish("udp", "", null, m);
        }
    }

    @Override
    public void setCompress(boolean compress) {
        this.compress = compress;
    }


}
