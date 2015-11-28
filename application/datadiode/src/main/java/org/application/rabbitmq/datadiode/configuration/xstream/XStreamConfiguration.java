package org.application.rabbitmq.datadiode.configuration.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by marcelmaatkamp on 26/10/15.
 */

@Configuration
public class XStreamConfiguration {
    @Bean
    JettisonMappedXmlDriver jettisonMappedXmlDriver() {
        JettisonMappedXmlDriver jettisonMappedXmlDriver = new JettisonMappedXmlDriver();
        return jettisonMappedXmlDriver;
    }

    @Bean
    XStream xstream() {
        XStream xStream = new XStream(jettisonMappedXmlDriver());
        xStream.setMode(XStream.NO_REFERENCES);
        return xStream;
    }
}
