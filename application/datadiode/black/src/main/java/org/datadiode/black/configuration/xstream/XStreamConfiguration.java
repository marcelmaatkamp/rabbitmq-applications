package org.datadiode.black.configuration.xstream;

        import com.thoughtworks.xstream.XStream;
        import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
        import org.datadiode.model.event.sensor.Sensor;
        import org.datadiode.model.event.sensor.SensorEvent;
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
        xStream.alias("sensor", Sensor.class);
        xStream.alias("sensorEvent", SensorEvent.class);
        return xStream;
    }
}
