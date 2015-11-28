package org.application.rabbitmq.datadiode.udp.internal.service;

import org.springframework.messaging.Message;

import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * Created by marcelmaatkamp on 27/10/15.
 */
public interface UdpReceiverService {

    public void udpMessage(Message message) throws IOException, DataFormatException;

    public void setCompress(boolean compress);
}
