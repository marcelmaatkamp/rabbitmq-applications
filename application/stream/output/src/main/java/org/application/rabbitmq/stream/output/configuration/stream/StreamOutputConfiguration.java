package org.application.rabbitmq.stream.output.configuration.stream;

import org.apache.commons.lang3.RandomUtils;
import org.application.rabbitmq.stream.model.Segment;
import org.application.rabbitmq.stream.model.SegmentHeader;
import org.application.rabbitmq.stream.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Configuration
public class StreamOutputConfiguration {
    private static final Logger log = LoggerFactory.getLogger(StreamOutputConfiguration.class);

    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

    int maxMessageSize = 700;

    Map<SegmentHeader ,List<Message>> uMessages = new HashMap();

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "cutter", durable = "true"),
                    exchange = @Exchange(value = "cutter"))
    )
    void reconstruct(Message message) throws IOException {
        Object o = SerializationUtils.deserialize(message.getBody());
        if(o instanceof SegmentHeader) {
            SegmentHeader segmentHeader = (SegmentHeader)o;
            uMessages.put(segmentHeader, new ArrayList());
            log.info("started("+segmentHeader.uuid+")");
        } else if(o instanceof Segment) {
            Segment segment = (Segment)o;
            for(SegmentHeader segmentHeader : uMessages.keySet()) {
                if(segmentHeader.uuid == segment.uuid) {
                    log.info("s("+segmentHeader.uuid+") - m("+segment.uuid+")");
                    List<Message> messages = uMessages.get(segmentHeader);
                    messages.add(message);
                    if(messages.size() == segmentHeader.count) {
                        Message messageFromStream = StreamUtils.reconstruct(messages);
                        rabbitTemplate.send("bla", null, messageFromStream);
                    } else {
                        log.info("uuid("+segmentHeader.uuid+"): " + messages.size());
                    }
                }
            }

        } else {
            log.error("Error: Unknown object: " + o);
        }
    }
}
