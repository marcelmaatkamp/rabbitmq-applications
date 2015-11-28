package org.application.rabbitmq.datadiode.udp.internal.configuration.unicast;

import org.application.rabbitmq.datadiode.udp.internal.service.UdpReceiverService;
import org.application.rabbitmq.datadiode.udp.internal.service.UdpReceiverServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Created by marcelmaatkamp on 27/10/15.
 */
@Configuration
public class UnicastConfiguration {
    private static final Logger log = LoggerFactory.getLogger(UnicastConfiguration.class);

    @Autowired
    Environment environment;

    @Bean
    UdpReceiverService udpReceiverService() {
        UdpReceiverService udpReceiverService = new UdpReceiverServiceImpl();
        udpReceiverService.setCompress(environment.getProperty("application.datadiode.red.udp.compress", Boolean.class));
        return udpReceiverService;
    }
}
