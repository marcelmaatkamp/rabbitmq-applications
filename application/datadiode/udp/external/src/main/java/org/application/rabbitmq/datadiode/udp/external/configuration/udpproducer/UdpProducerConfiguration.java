package org.application.rabbitmq.datadiode.udp.external.configuration.udpproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.spring.context.config.EnableReactor;

/**
 * Created by marcel on 25-09-15.
 */
@Configuration
@EnableConfigurationProperties(UdpProducerConfiguration.UdpProducerConfigurationProperties.class)
@EnableReactor
@EnableScheduling
public class UdpProducerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(UdpProducerConfiguration.class);

    @Autowired
    UdpProducerConfigurationProperties udpProducerConfigurationProperties;

    @Bean
    UnicastSendingMessageHandler unicastSendingMessageHandler() {
        UnicastSendingMessageHandler unicastSendingMessageHandler =
                new UnicastSendingMessageHandler(
                        udpProducerConfigurationProperties.getHost(),
                        udpProducerConfigurationProperties.getPort());

        log.info("setSoSendBufferSize:"+ unicastSendingMessageHandler.getSoSendBufferSize());
        log.info("isAcknowledge:"+ unicastSendingMessageHandler.isAcknowledge());
        log.info("isCountsEnabled:"+ unicastSendingMessageHandler.isCountsEnabled());
        log.info("isLoggingEnabled:"+ unicastSendingMessageHandler.isLoggingEnabled());
        log.info("isStatsEnabled:"+ unicastSendingMessageHandler.isStatsEnabled());

        unicastSendingMessageHandler.setSoSendBufferSize(udpProducerConfigurationProperties.getSoSendBufferSize()); // keeps udp flow constant
        unicastSendingMessageHandler.setStatsEnabled(false);
        unicastSendingMessageHandler.setLoggingEnabled(false);
        unicastSendingMessageHandler.setSoSendBufferSize(16384);

        if(log.isDebugEnabled()) {
            log.debug("sending to " +udpProducerConfigurationProperties.getHost()+":" +udpProducerConfigurationProperties.getPort());
        }
        return unicastSendingMessageHandler;
    }

    @ConfigurationProperties(prefix = "application.datadiode.udp.external")
    public static class UdpProducerConfigurationProperties {
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
