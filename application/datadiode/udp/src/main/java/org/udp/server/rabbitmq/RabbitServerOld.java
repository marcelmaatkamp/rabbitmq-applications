package org.udp.server.rabbitmq;


import com.google.common.primitives.Ints;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by marcel on 06-12-15.
 */

public class RabbitServerOld {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RabbitServerOld.class);

    static int serverPort = 9999;
    static int packetSize = 1300;

    static byte[] b = new byte[packetSize];
    static byte[] indexBytes = new byte[4];
    static int oldIndex = -1;


    ConnectionFactory factory;
    Connection conn;
    Channel channel;

    RabbitServerOld() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setHost("localhost");
        factory.setPort(5674);
        conn = factory.newConnection();
        channel = conn.createChannel();


        DatagramChannel channel = DatagramChannel.open();
        DatagramSocket socket = channel.socket();
        socket.setReceiveBufferSize(8192 * 128); // THIS!

        SocketAddress address = new InetSocketAddress(serverPort);
        socket.bind(address);

        byte[] message = new byte[packetSize];
        AtomicInteger atomicInteger = new AtomicInteger(0);

        ServerThread serverThread = new ServerThread(atomicInteger);
        serverThread.start();
        log.info("receiving: " + serverPort + " " + socket);

        try {


            while (true) {

                DatagramPacket packet = new DatagramPacket(message, message.length);
                socket.receive(packet);
                atomicInteger.incrementAndGet();

                // log.info("["+atomicInteger.get()+"] RabbitServer received "+ +packet.getLength());

                byte[] m = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                this.channel.basicPublish("udp", "", null, m);

                for (int i = 0; i < 4; i++) {
                    indexBytes[i] = m[i];
                }
                int index = Ints.fromByteArray(m);

                if (oldIndex != -1 && index != 0 && index != (oldIndex + 1)) {
                    log.warn("packet loss: " + index + ", " + oldIndex);
                }
                oldIndex = index;
                // log.info("RabbitServer received "+ +b.length+": " + new String(Base64.encodeBase64(b)));

            }
        } finally {
            log.info("received: " + atomicInteger.get());
        }
    }

    public static void main(String[] args) throws Exception {
        RabbitServerOld server = new RabbitServerOld();
    }


    class ServerThread extends Thread {

        private final org.slf4j.Logger log = LoggerFactory.getLogger(ServerThread.class);

        AtomicInteger atomicInteger;
        int old = 0;

        public ServerThread(AtomicInteger atomicInteger) throws SocketException {
            this.atomicInteger = atomicInteger;
        }

        public void run() {
            while (true) {
                log.info("packets: " + atomicInteger.get() + " (" + (atomicInteger.get() - old) + ")");
                old = atomicInteger.get();
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
