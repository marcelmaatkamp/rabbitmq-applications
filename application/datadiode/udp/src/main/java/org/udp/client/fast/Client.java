package org.udp.client.fast;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Date;

/**
 * send 1048576 packets of 8192 bytes = 8192 MB in 72.221 secs = 14518 pkts/sec or 113.0 MB/sec
 * <p>
 * Created by marcel on 06-12-15.
 */
public class Client {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Client.class);

    static String hostname = "192.168.2.255";
    static int port = 9999;

    // (8192 * 1024 * 256)/1024/1024 = 2048 = 2G
    int packetSize = 8192;
    int packetCount = 1024 * 1024;
    int packetRate = 14500;

    Client() throws UnknownHostException, SocketException {
        InetAddress ia = InetAddress.getByName(hostname);
        ClientThread clientThread = new ClientThread(ia, port);
        clientThread.start();
        log.info("sending " + hostname + "(" + ia + "):" + port);
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
        } catch(Exception e) {
        }
    }


    class ClientThread extends Thread {

        final RateLimiter rateLimiter = RateLimiter.create(packetRate);
        private final org.slf4j.Logger log = LoggerFactory.getLogger(ClientThread.class);
        private InetAddress server;
        private DatagramSocket socket;
        private boolean stopped = false;
        private int port;

        public ClientThread(InetAddress address, int port) throws SocketException {
            this.server = address;
            this.port = port;
            this.socket = new DatagramSocket();
            this.socket.connect(server, port);
        }

        public void halt() {
            this.stopped = true;
        }

        public DatagramSocket getSocket() {
            return this.socket;
        }

        public void run() {

            int index = 0;
            try {
                byte[] array = RandomUtils.nextBytes(packetSize);

                int items = packetCount;
                Date old = new Date();
                while (items > 0) {
                    byte[] indexBytes = Ints.toByteArray(index);

                    for (int i = 0; i < 4; i++) {
                        array[i] = indexBytes[i];
                    }

                    DatagramPacket output = new DatagramPacket(array, array.length, server, port);
                    index++;

                    boolean success = false;
                    while (!success) {
                        try {
                            socket.send(output);
                        } catch (IOException e) {
                            log.error("Exception: ", e);
                        }
                        success = true;
                    }
                    items = items - 1;
                    rateLimiter.acquire();
                }

                Date now = new Date();

                double secs = ((double) (now.getTime() - old.getTime())) / 1000;
                long pkts = (((long) ((long) packetCount * (long) packetSize)) / 1024 / 1024);
                double pkt_secs = Math.round(((double) packetCount) / (double) secs);
                double mb_secs = Math.round(((double) (pkt_secs * packetSize) / 1024 / 1024));

                log.info("transferred " + packetCount + " packets of " + packetSize + " bytes = " + pkts + " MB in " + secs + " secs = " + pkt_secs + " pkts/sec or " + mb_secs + " MB/sec");
            } catch (Exception ex) {
                log.error("Exception: ", ex);
            }
        }
    }
}
