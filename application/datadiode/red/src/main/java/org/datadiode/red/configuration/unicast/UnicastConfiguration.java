package org.datadiode.red.configuration.unicast;

import org.datadiode.red.service.UdpReceiverService;
import org.datadiode.red.service.UdpReceiverServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Created by marcelmaatkamp on 27/10/15.
 */
@Configuration
public class UnicastConfiguration {
    @Autowired
    Environment environment;

    @Bean
    UdpReceiverService encryptedUdpReceiverService() {
        UdpReceiverService udpReceiverService = new UdpReceiverServiceImpl();
        udpReceiverService.setCompress(environment.getProperty("application.datadiode.red.udp.compress", Boolean.class));
        return udpReceiverService;
    }
}
