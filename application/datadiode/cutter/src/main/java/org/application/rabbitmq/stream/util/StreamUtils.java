package org.application.rabbitmq.stream.util;

import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.stream.model.Segment;
import org.application.rabbitmq.stream.model.SegmentHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Component
public class StreamUtils {
    private static final Logger log = LoggerFactory.getLogger(StreamUtils.class);
    private static MessageDigest messageDigest;
    @Autowired
    XStream xStream;
    @Autowired
    MessageDigest _messageDigest;
    @Value(value = "${application.stream.cutter.digest}")
    boolean doDigest;

    private static void addRedundantly(List<Message> messages, Message message, int redundancyFactor) {
        for (int i = 0; i < redundancyFactor; i++) {
            messages.add(message);
        }
    }

    public static List<Message> cut(Message message, int bufSize, int redundancyFactor) {
        List<Message> results = new ArrayList();

        byte[] messageBytes = SerializationUtils.serialize(message);
        messageDigest.update(messageBytes);

        int aantal = (int) (messageBytes.length / bufSize);
        int modulo = messageBytes.length % bufSize;

        SegmentHeader sh = new SegmentHeader().
                size(messageBytes.length).
                blockSize(bufSize).
                count(aantal).
                digest(messageDigest.digest());

        MessageProperties messageProperties = new MessageProperties();
        // messageProperties.setReceivedRoutingKey(sh.uuid.toString());
        messageProperties.getHeaders().put("type", sh.getClass());
        messageProperties.getHeaders().put("uuid", sh.uuid);
        messageProperties.getHeaders().put("block", sh.blockSize);
        messageProperties.getHeaders().put("count", sh.count);
        messageProperties.getHeaders().put("size", sh.size);

        // results.add(new Message(SerializationUtils.serialize(sh), messageProperties));
        List<Message> headers = new ArrayList();
        addRedundantly(headers, new Message(SerializationUtils.serialize(sh), messageProperties), redundancyFactor);

        // blocksize
        for (int i = 0; i < aantal; i++) {
            int start = i * bufSize;
            int stop = start + bufSize;
            Segment segment = new Segment().index(i).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, start, stop));
            messageProperties = new MessageProperties();
            messageProperties.getHeaders().put("type", segment.getClass());
            messageProperties.getHeaders().put("uuid", segment.uuid);
            messageProperties.getHeaders().put("index", segment.index);
            messageProperties.getHeaders().put("count", sh.count);
            messageProperties.getHeaders().put("size", segment.segment.length);
            results.add(new Message(SerializationUtils.serialize(segment), messageProperties));
            addRedundantly(results, new Message(SerializationUtils.serialize(segment), messageProperties), redundancyFactor);
        }

        // and the rest
        if (modulo > 0) {
            int start = aantal * bufSize;
            int stop = modulo;
            Segment segment = new Segment().index(aantal).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, aantal * bufSize, aantal * bufSize + modulo));
            messageProperties = new MessageProperties();
            messageProperties.getHeaders().put("type", segment.getClass());
            messageProperties.getHeaders().put("uuid", segment.uuid);
            messageProperties.getHeaders().put("index", segment.index);
            messageProperties.getHeaders().put("count", sh.count);
            messageProperties.getHeaders().put("size", segment.segment.length);
            results.add(new Message(SerializationUtils.serialize(segment), messageProperties));
            addRedundantly(results, new Message(SerializationUtils.serialize(segment), messageProperties), redundancyFactor);
        }

        Collections.shuffle(results);
        headers.addAll(results);

        return headers;
    }

    public static Message reconstruct(SegmentHeader segmentHeader, Set<Segment> segments) throws IOException {
        // SegmentHeader segmentHeader = (SegmentHeader) SerializationUtils.deserialize(messages.get(0).getBody());

        byte[] buffer = new byte[segmentHeader.size];
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();

        for (Segment segment : segments) {
            bos2.write(segment.segment);
        }
        bos2.close();

        byte[] data = bos2.toByteArray();
        messageDigest.update(data);

        // compare digest
        if (Arrays.equals(messageDigest.digest(), segmentHeader.digest)) {
            Message message = (Message) SerializationUtils.deserialize(data);
            return message;
        } else {
            log.error("ERROR: Message digest did not match!");
        }
        return null;
    }

    @PostConstruct
    public void init() {
        messageDigest = _messageDigest;
    }


}
