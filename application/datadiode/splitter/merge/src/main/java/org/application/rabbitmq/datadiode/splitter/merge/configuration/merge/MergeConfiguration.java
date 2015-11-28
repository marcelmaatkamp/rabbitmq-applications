package org.application.rabbitmq.datadiode.splitter.merge.configuration.merge;

import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.stream.model.Segment;
import org.application.rabbitmq.stream.model.SegmentHeader;
import org.application.rabbitmq.stream.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Configuration
@EnableScheduling
public class MergeConfiguration implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(MergeConfiguration.class);
    Map<SegmentHeader, TreeSet<Segment>> uMessages = new ConcurrentHashMap();
    @Autowired
    XStream xStream;
    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

    @Bean
    MessageDigest messageDigest() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest;
    }

    @Bean
    StreamUtils streamUtils() {
        StreamUtils streamUtils = new StreamUtils();
        return streamUtils;
    }

    @Bean
    org.springframework.amqp.core.Exchange reconstructExchange() {
        org.springframework.amqp.core.Exchange exchange = new FanoutExchange("split");
        return exchange;
    }

    @Bean
    org.springframework.amqp.core.Queue cutterQueue() {
        org.springframework.amqp.core.Queue queue = new org.springframework.amqp.core.Queue("split");
        return queue;

    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "splitter", durable = "true"),
                    exchange = @Exchange(value = "splitter", durable = "true", autoDelete = "false", type = "fanout"))
    )
    public void onMessage(Message message) {
        Object o = SerializationUtils.deserialize(message.getBody());
        if (log.isDebugEnabled()) {
            log.debug("o(" + xStream.toXML(o) + ")");
        }
        if (o instanceof SegmentHeader) {
            SegmentHeader segmentHeader = (SegmentHeader) o;
            if (log.isDebugEnabled()) {
                log.debug("header(" + segmentHeader.uuid + ") of size(" + segmentHeader.blockSize + ") and count(" + segmentHeader.count + ")");
            }
            boolean found = false;
            for (SegmentHeader s : uMessages.keySet()) {
                if (s.uuid.equals(segmentHeader.uuid)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                uMessages.put(segmentHeader, new TreeSet<Segment>());
                if (log.isDebugEnabled()) {
                    log.debug("starting message(" + segmentHeader.uuid + ") of size(" + segmentHeader.blockSize + ") and count(" + segmentHeader.count + ")");
                }
            }
        } else if (o instanceof Segment) {

            Segment segment = (Segment) o;
            if (log.isDebugEnabled()) {
                log.debug("segment(" + xStream.toXML(segment) + ")");
            }
            for (SegmentHeader segmentHeader : uMessages.keySet()) {
                if (segmentHeader.uuid.equals(segment.uuid)) {
                    segmentHeader.update = new Date();
                    Set<Segment> messages = uMessages.get(segmentHeader);

                    messages.add(segment);
                    if (messages.size() == segmentHeader.count + 1) {
                        try {
                            Message messageFromStream = StreamUtils.reconstruct(segmentHeader, messages);
                            log.info("reconstructed: " + messageFromStream);
                            rabbitTemplate.send(messageFromStream);
                            uMessages.remove(segmentHeader);
                        } catch (IOException e) {
                            log.error("Exception: " + e);
                        }
                    }
                }
            }
        } else {
            log.error("Error: Unknown object: " + o);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void cleanup() throws MalformedURLException {
        if (uMessages.keySet().size() > 0) {
            log.info("concurrent active messages: " + uMessages.keySet().size());
        }

        for (SegmentHeader segmentHeader : uMessages.keySet()) {
            if (segmentHeader.update != null && (new Date().getTime() - segmentHeader.update.getTime()) > 25000) {
                log.info("cleaning up " + segmentHeader.uuid + ", got(" + uMessages.get(segmentHeader).size() + "), missing(" + ((segmentHeader.count + 2) - uMessages.get(segmentHeader).size()) + ")");
                uMessages.remove(segmentHeader);
            }

        }
    }
}
