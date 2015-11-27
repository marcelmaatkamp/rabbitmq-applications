package org.datadiode.red.configuration.unicast;

import org.datadiode.red.configuration.rabbitmq.RabbitmqConfiguration;
import org.datadiode.red.service.UdpReceiverService;
import org.datadiode.red.service.UdpReceiverServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Created by marcelmaatkamp on 27/10/15.
 */
@Configuration
@Import(RabbitmqConfiguration.class)
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
