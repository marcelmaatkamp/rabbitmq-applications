package org.application.rabbitmq.datadiode.cutter.util;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.application.rabbitmq.datadiode.cutter.model.Segment;
import org.application.rabbitmq.datadiode.cutter.model.SegmentHeader;
import org.application.rabbitmq.datadiode.cutter.model.SegmentType;
import org.application.rabbitmq.datadiode.model.message.ExchangeMessage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Created by marcel on 04-12-15.
 */
public class StreamUtilsTest {
    private static final Logger log = LoggerFactory.getLogger(StreamUtilsTest.class);


    @Test
    public void testCut() throws Exception {
        Map<SegmentHeader, TreeSet<Segment>> uMessages = new ConcurrentHashMap();

        boolean calculateDigest = true;
        byte[] array = RandomUtils.nextBytes(65535);

        String digestName = "SHA-256";

        MessageProperties mp = new MessageProperties();
        Message m = new Message(array, mp);
        ExchangeMessage em = new ExchangeMessage(m, "exchange-data");
        List<Message> ms = StreamUtils.cut(em, 8730, 2, calculateDigest, digestName);

        for(Message message : ms) {
            byte[] segment_or_header = message.getBody();
            ByteArrayInputStream bis = new ByteArrayInputStream(segment_or_header);


            Segment segment = Segment.fromByteArray(bis, segment_or_header);
            for (SegmentHeader segmentHeader : uMessages.keySet()) {
                if (segmentHeader.uuid.equals(segment.uuid)) {

                        if (log.isDebugEnabled()) {
                            log.debug("sh[" + segmentHeader.uuid.toString() + "]: ss[" + segment.uuid.toString() + "] index[" + segment.index + "]: count: " + uMessages.get(segmentHeader).size());
                        }
                        segmentHeader.update = new Date();
                        Set<Segment> messages = uMessages.get(segmentHeader);
                        if (segment != null && messages != null) {
                            messages.add(segment);
                            if (messages.size() == segmentHeader.count) {
                                ExchangeMessage messageFromStream = StreamUtils.reconstruct(messages, calculateDigest, digestName);
                                log.info(ReflectionToStringBuilder.toString(messageFromStream));
                            }
                        }
                    }
                }

            }
        }




    @Test
    public void testReconstruct() throws Exception {

    }
}