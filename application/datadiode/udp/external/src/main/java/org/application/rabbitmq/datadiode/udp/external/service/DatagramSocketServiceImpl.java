package org.application.rabbitmq.datadiode.udp.external.service;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by marcelmaatkamp on 07/12/15.
 */
public class DatagramSocketServiceImpl implements DatagramSocketService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DatagramSocketServiceImpl.class);

    private final DatagramSocket datagramSocket;

    private RateLimiter rateLimiter;

    public void setRate(double rate) {
        rateLimiter = RateLimiter.create(rate);
    }

    public DatagramSocketServiceImpl(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public void send(byte[] array) throws IOException {
        DatagramPacket output = new DatagramPacket(array, array.length);
        rateLimiter.acquire(1);
        datagramSocket.send(output);
    }

}
