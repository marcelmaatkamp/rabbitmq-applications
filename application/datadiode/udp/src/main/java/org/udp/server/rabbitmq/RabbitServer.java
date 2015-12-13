package org.udp.server.rabbitmq;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by marcel on 06-12-15.
 */

public class RabbitServer {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RabbitServer.class);

    static int serverPort = 9999;
    static int packetSize = 8192;

    static byte[] b = new byte[packetSize];
    static byte[] indexBytes = new byte[4];
    static int oldIndex = -1;

    ConnectionFactory factory;
    Connection conn;
    Channel channel;

    ConcurrentLinkedQueue<byte[]> concurrentLinkedQueue = new ConcurrentLinkedQueue();

    RabbitServer() throws IOException, TimeoutException {

        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("192.168.178.13");
        factory.setUsername("guest");
        factory.setPassword("guest");
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

        StatsThread statsThread = new StatsThread(atomicInteger);
        statsThread.start();

        log.info("receiving: " + serverPort + " " + socket + ", sending: " + factory + ",  " + conn + ", " + channel);

        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(message, message.length);
                socket.receive(packet);
                atomicInteger.incrementAndGet();

                byte[] m = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                concurrentLinkedQueue.add(m);
            }
        } finally {
            log.info("received: " + atomicInteger.get());
        }
    }

    public static void main(String[] args) throws Exception {
        RabbitServer rabbitServer = new RabbitServer();
    }


    class StatsThread extends Thread {

        private final org.slf4j.Logger log = LoggerFactory.getLogger(StatsThread.class);

        AtomicInteger atomicInteger;
        int prev = 0;
        int total = 0;

        public StatsThread(AtomicInteger atomicInteger) throws SocketException {
            this.atomicInteger = atomicInteger;
        }

        public void run() {
            while (true) {
                int now = atomicInteger.get();
                int diff = (now - prev);

                if (diff > 0) {
                    total = total + diff;
                    log.info("packets: diff(" + diff + "), total in session(" + total + "), total(" + atomicInteger.get() + ")");
                    prev = now;
                } else if (total > 0) {
                    log.info("----------------------- ");
                    log.info("total packets received: " + total);
                    log.info("----------------------- ");
                    total = 0;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    class SendThread extends Thread {
        private final org.slf4j.Logger log = LoggerFactory.getLogger(SendThread.class);

        ConcurrentLinkedQueue<byte[]> concurrentLinkedQueue;
        Channel channel;

        public SendThread(ConcurrentLinkedQueue<byte[]> concurrentLinkedQueue, Channel channel) throws SocketException {
            this.concurrentLinkedQueue = concurrentLinkedQueue;
            this.channel = channel;
        }

        public void run() {
            while (true) {
                try {
                    for(byte[] msg : concurrentLinkedQueue) {
                        this.channel.basicPublish("udp", "", null, msg);
                        concurrentLinkedQueue.remove(msg);
                    }
                    Thread.sleep(10);
                }catch(Exception e) {
                    log.error("Exception: ", e);
                }
            }
        }
    }

}
