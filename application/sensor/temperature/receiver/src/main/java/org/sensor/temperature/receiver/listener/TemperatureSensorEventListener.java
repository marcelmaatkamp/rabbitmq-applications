package org.sensor.temperature.receiver.listener;

import com.rabbitmq.client.Channel;
import com.thoughtworks.xstream.XStream;
import org.event.model.sensor.SensorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by marcelmaatkamp on 15/10/15.
 */
public class TemperatureSensorEventListener implements ChannelAwareMessageListener {
    private static final Logger log = LoggerFactory.getLogger(TemperatureSensorEventListener.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    XStream xStream;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        Object o = rabbitTemplate.getMessageConverter().fromMessage(message) ;
        if(o instanceof SensorEvent) {
            SensorEvent sensorEvent = (SensorEvent) rabbitTemplate.getMessageConverter().fromMessage(message);
            if(log.isDebugEnabled()) {
                log.debug("sensorEvent: " + xStream.toXML(sensorEvent));
            }
        } else if ( o instanceof Message ){
            log.info("m: " + (Message)o);

        }else if ( o instanceof byte[] ){
            log.info("b: " + new String((byte[])o, "UTF-8"));

        }
    }
}
