package org.application.rabbitmq.splitter.split.configuration.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import org.application.rabbitmq.stream.model.Segment;
import org.application.rabbitmq.stream.model.SegmentHeader;
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
        xStream.alias("segment", Segment.class);
        xStream.alias("segmentHeader", SegmentHeader.class);
        return xStream;
    }
}
