package org.application.rabbitmq.datadiode.udp.external.configuration.socket;

import org.application.rabbitmq.datadiode.udp.external.service.DatagramSocketService;
import org.application.rabbitmq.datadiode.udp.external.service.DatagramSocketServiceImpl;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.*;

/**
 * Created by marcel on 06-12-15.
 */
@Configuration
@EnableConfigurationProperties(DatagramSocketConfiguration.DatagramSocketConfigurationProperties.class)
public class DatagramSocketConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DatagramSocketConfiguration.class);

    @Autowired
    DatagramSocketConfigurationProperties datagramSocketConfigurationProperties;

    @Bean
    DatagramSocket datagramSocket() throws SocketException, UnknownHostException {
        DatagramSocket socket = new DatagramSocket();
        socket.connect(inetAddress(), datagramSocketConfigurationProperties.getPort());
        return socket;
    }

    @Bean
    InetAddress inetAddress() throws UnknownHostException {
        return InetAddress.getByName(datagramSocketConfigurationProperties.getHost());
    }

    @Bean
    DatagramSocketService datagramSocketService() throws SocketException, UnknownHostException {
        DatagramSocketService datagramSocketService = new DatagramSocketServiceImpl(datagramSocket());
        return datagramSocketService;
    }


    @ConfigurationProperties(prefix = "application.datadiode.udp.external")
    public static class DatagramSocketConfigurationProperties {
        String host;
        int port;

        public int getSoSendBufferSize() {
            return soSendBufferSize;
        }

        public void setSoSendBufferSize(int soSendBufferSize) {
            this.soSendBufferSize = soSendBufferSize;
        }

        int soSendBufferSize;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

    }

}
