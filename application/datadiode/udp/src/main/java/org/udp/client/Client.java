package org.udp.client;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by marcel on 06-12-15.
 */
public class Client {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Client.class);

    static String hostname = "docker";
    static int port = 1235;

    public static void main(String[] args) throws Exception {
        InetAddress ia = InetAddress.getByName(hostname);
        SenderThread sender = new SenderThread(ia, port);
        sender.start();
    }

}

class SenderThread extends Thread {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SenderThread.class);

    private InetAddress server;
    private DatagramSocket socket;
    private boolean stopped = false;
    private int port;

    public SenderThread(InetAddress address, int port) throws SocketException {
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

    final RateLimiter rateLimiter = RateLimiter.create(100000);
    // UDP Exchange:
    // 8192 - 10150 = 91MB/sec

    // Raw
    //  9000 - 13173.0 = 113.2MB/sec
    //  8192 - 15150.0 = 118.4MB/sec
    int pkt_size = 8192;

    public void run() {

        int index = 0;
        try {
            byte[] array = RandomUtils.nextBytes(pkt_size);
            int count = 1024*1;

            int items = count;
            Date old = new Date();
            while (items > 0) {
                byte[] indexBytes = Ints.toByteArray(index);

                for(int i = 0; i < 4; i++) {
                    array[i] = indexBytes[i];
                }

                DatagramPacket output = new DatagramPacket(array, array.length, server, port);
                index++;

                socket.send(output);
                // Thread.yield();
                items = items -1;
                rateLimiter.acquire();
            }

            Date now = new Date();
            double secs = ((double)(now.getTime()-old.getTime()))/1000;
            double pkt_secs = ((double)count)/(double)secs;

            log.info("send " + count + " in " +secs+ " secs:  " + ((double)count)/(double)secs +" = " + ((double)(pkt_secs * pkt_size)/1024/1024) );
        }
        catch (Exception ex) {
            log.error("Exception: " ,ex);
        }
    }
}
