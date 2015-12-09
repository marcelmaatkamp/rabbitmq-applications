package org.udp.server;


import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


/**
 * Created by marcel on 06-12-15.
 */

public class Server {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Server.class);

    static byte[] b = new byte[8192];
    static byte[] indexBytes = new byte[4];
    static int oldIndex = -1;

    Server() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        DatagramSocket socket = channel.socket();
        SocketAddress address = new InetSocketAddress(9999);
        socket.bind(address);

        byte[] message = new byte[8192];
        AtomicInteger atomicInteger = new AtomicInteger(0);

        ClientThread clientThread = new ClientThread(atomicInteger);
        clientThread.run();


        try {


            while (true) {

                DatagramPacket packet = new DatagramPacket(message, message.length);
                socket.receive(packet);
                // log.info("Server received "+ +packet.getLength());

                byte[] m = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

                for (int i = 0; i < 4; i++) {
                    indexBytes[i] = m[i];
                }
                int index = Ints.fromByteArray(m);

                if (oldIndex != -1 && index != 0 && index != (oldIndex + 1)) {
                    log.warn("packet loss: " + index + ", " + oldIndex);
                }
                oldIndex = index;
                atomicInteger.incrementAndGet();
                // log.info("Server received "+ +b.length+": " + new String(Base64.encodeBase64(b)));

            }
        } finally {
            log.info("received: " + atomicInteger.get());
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
    }


    class ClientThread extends Thread {

        private final org.slf4j.Logger log = LoggerFactory.getLogger(ClientThread.class);

        AtomicInteger atomicInteger;

        public ClientThread(AtomicInteger atomicInteger) throws SocketException {
            this.atomicInteger = atomicInteger;
        }

        public void run() {
            while(true) {
                log.info("packets: " + atomicInteger.get());
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
