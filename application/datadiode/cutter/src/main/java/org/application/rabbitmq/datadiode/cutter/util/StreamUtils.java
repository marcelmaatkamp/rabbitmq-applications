package org.application.rabbitmq.datadiode.cutter.util;

import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.cutter.model.Segment;
import org.application.rabbitmq.datadiode.model.message.ExchangeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Component
public class StreamUtils {
    private static final Logger log = LoggerFactory.getLogger(StreamUtils.class);
    static Gson gson;
    private static XStream xStream;
    @Autowired
    XStream _xStream;
    @Autowired
    Gson _gson;

    private static void addRedundantly(List<Message> messages, Message message, int redundancyFactor) {
        for (int i = 0; i < redundancyFactor; i++) {
            messages.add(message);
        }
    }

    // todo: msg fully loaded
    public static List<Message> cut(ExchangeMessage message, int bufSize, int redundancyFactor, boolean calculateDigest, String digestName) throws IOException, NoSuchAlgorithmException {
        List<Message> results = new ArrayList();

        if (calculateDigest) {
            MessageDigest messageDigest = MessageDigest.getInstance(digestName);
            messageDigest.update(message.getMessage().getBody());
            message.setDigest(messageDigest.digest());
        }

        byte[] messageBytes = SerializationUtils.serialize(message);

        int count = (int) (messageBytes.length / bufSize);
        int modulo = messageBytes.length % bufSize;
        MessageProperties messageProperties = message.getMessage().getMessageProperties();

        /*
        SegmentHeader sh = new SegmentHeader().
                size(messageBytes.length).
                blockSize(bufSize).
                count(aantal);


        if(calculateDigest) {
            MessageDigest messageDigest = MessageDigest.getInstance(digestName);
            messageDigest.update(messageBytes);
            sh.digest(messageDigest.digest());
        }


        MessageProperties messageProperties = message.getMessage().getMessageProperties();
        messageProperties.getHeaders().put("type", sh.getClass());
        messageProperties.getHeaders().put("uuid", sh.uuid);
        messageProperties.getHeaders().put("block", sh.blockSize);
        messageProperties.getHeaders().put("count", sh.count);
        messageProperties.getHeaders().put("size", sh.size);

        List<Message> headers = new ArrayList();
        if(log.isDebugEnabled()) {
            log.debug("[" + sh.uuid.toString() + "]:  sh(" + sh.toByteArray(calculateDigest).length + ")");
        }
        addRedundantly(headers, new Message(sh.toByteArray(calculateDigest), messageProperties), redundancyFactor);
        */
        // blocksize
        for (int i = 0; i < count; i++) {
            int start = i * bufSize;
            int stop = start + bufSize;
            Segment segment = new Segment().count(count + 1).index(i).uuid(message.getUuid()).segment(Arrays.copyOfRange(messageBytes, start, stop));
            messageProperties = new MessageProperties();
            messageProperties.getHeaders().put("type", segment.getClass());
            messageProperties.getHeaders().put("uuid", segment.uuid);
            messageProperties.getHeaders().put("index", segment.index);
            messageProperties.getHeaders().put("count", count);
            messageProperties.getHeaders().put("size", segment.segment.length);
            if (log.isDebugEnabled()) {
                log.debug(segment.toString() + " buf(" + bufSize + ").start(" + start + ").stop(" + stop + ").segment(" + segment.toByteArray().length + ")");
            }
            addRedundantly(results, new Message(segment.toByteArray(), messageProperties), redundancyFactor);
        }

        // and the rest
        if (modulo > 0) {
            int start = count * bufSize;
            int stop = count * bufSize + modulo;
            Segment segment = new Segment().count(count + 1).index(count).uuid(message.getUuid()).segment(Arrays.copyOfRange(messageBytes, start, stop));
            messageProperties = new MessageProperties();
            messageProperties.getHeaders().put("type", segment.getClass());
            messageProperties.getHeaders().put("uuid", segment.uuid);
            messageProperties.getHeaders().put("index", segment.index);
            messageProperties.getHeaders().put("count", count);
            messageProperties.getHeaders().put("size", segment.segment.length);
            messageProperties.getHeaders().put("size", modulo);

            if (log.isDebugEnabled()) {
                log.debug(segment.toString() + " buf(" + bufSize + ").start(" + start + ").stop(" + stop + ").segment(" + segment.toByteArray().length + ")");
            }
            addRedundantly(results, new Message(segment.toByteArray(), messageProperties), redundancyFactor);
        }

        Collections.shuffle(results);

        if (log.isDebugEnabled()) {
            log.debug("[" + message.getUuid().toString() + "]: serialize.length: " + messageBytes.length + ", in count(" + results.size() + ")");
        }


        return results;
    }

    public static ExchangeMessage reconstruct(Set<Segment> segments, boolean doDigest, String digestName) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();

        UUID uuid = null;
        for (Segment segment : segments) {
            if (log.isDebugEnabled()) {
                if (uuid == null) {
                    uuid = segment.uuid;
                }
                log.debug("adding " + segment.toString());
            }
            bos2.write(segment.segment);
        }
        bos2.close();

        try {
            if (log.isDebugEnabled()) {
                log.debug("[" + uuid.toString() + "]: serialize.length: " + bos2.toByteArray().length + ", in count(" + segments.size() + ")");
            }
            ExchangeMessage message = (ExchangeMessage) SerializationUtils.deserialize(bos2.toByteArray());
            MessageDigest messageDigest = MessageDigest.getInstance(digestName);
            messageDigest.update(message.getMessage().getBody());

            // compare digest
            if (MessageDigest.isEqual(messageDigest.digest(), message.getDigest())) {
                return message;
            } else {
                log.error("ERROR: uuid(" + message.getUuid().toString() + "): message.digest(" + Arrays.toString(message.getDigest()) + ") vs actual(" + Arrays.toString(messageDigest.digest()) + ") did not match: ");
            }
        } catch (IllegalArgumentException e) {
            log.error("Exception: ", e);
        }

        return null;
    }

    @PostConstruct
    public void init() {
        xStream = _xStream;
        gson = _gson;
    }

}
