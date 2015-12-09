package org.application.rabbitmq.datadiode.udp.internal.service;

import org.springframework.messaging.Message;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.zip.DataFormatException;

/**
 * Created by marcelmaatkamp on 27/10/15.
 */
public interface UdpReceiverService {

    public void start() throws IOException, TimeoutException;

    public void setCompress(boolean compress);
}
