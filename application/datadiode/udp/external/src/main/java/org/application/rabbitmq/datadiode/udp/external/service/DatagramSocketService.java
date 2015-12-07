package org.application.rabbitmq.datadiode.udp.external.service;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by marcelmaatkamp on 07/12/15.
 */
public interface DatagramSocketService {

    public void send(byte[] array) throws IOException;

}
