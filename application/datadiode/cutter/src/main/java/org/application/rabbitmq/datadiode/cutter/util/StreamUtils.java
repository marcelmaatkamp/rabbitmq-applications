package org.application.rabbitmq.datadiode.cutter.util;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.codec.binary.*;
import org.application.rabbitmq.datadiode.model.message.ExchangeMessage;
import org.application.rabbitmq.datadiode.cutter.model.Segment;
import org.application.rabbitmq.datadiode.cutter.model.SegmentHeader;
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
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Component
public class StreamUtils {
    private static final Logger log = LoggerFactory.getLogger(StreamUtils.class);

    private static XStream xStream;

    @Autowired
    XStream _xStream;

    @Autowired
    Gson _gson;

    static Gson gson;


    private static void addRedundantly(List<Message> messages, Message message, int redundancyFactor) {
        for (int i = 0; i < redundancyFactor; i++) {
            messages.add(message);
        }
    }

    // todo: msg fully loaded
    public static List<Message> cut(ExchangeMessage message, int bufSize, int redundancyFactor, boolean calculateDigest, String digestName) throws IOException, NoSuchAlgorithmException {
        List<Message> results = new ArrayList();

        byte[] messageBytes = SerializationUtils.serialize(message);

        int aantal = (int) (messageBytes.length / bufSize);
        int modulo = messageBytes.length % bufSize;

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
        // addRedundantly(headers, new Message(SerializationUtils.serialize(sh), messageProperties), redundancyFactor);

        addRedundantly(headers, new Message(sh.toByteArray(calculateDigest), messageProperties), redundancyFactor);

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

            // addRedundantly(results, new Message(SerializationUtils.serialize(segment), messageProperties), redundancyFactor);
            addRedundantly(results, new Message(segment.toByteArray(), messageProperties), redundancyFactor);
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

            // addRedundantly(results, new Message(SerializationUtils.serialize(segment), messageProperties), redundancyFactor);
            addRedundantly(results, new Message(segment.toByteArray(), messageProperties), redundancyFactor);

        }

        Collections.shuffle(results);
        headers.addAll(results);

        return headers;
    }

    public static ExchangeMessage reconstruct(SegmentHeader segmentHeader, Set<Segment> segments, boolean doDigest, String digestName) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();

        for (Segment segment : segments) {
            bos2.write(segment.segment);
        }
        bos2.close();

        byte[] data = bos2.toByteArray();
        MessageDigest messageDigest = MessageDigest.getInstance(digestName);
        messageDigest.update(data);

        // compare digest
        if (MessageDigest.isEqual(messageDigest.digest(), segmentHeader.digest)) {
            ExchangeMessage message = (ExchangeMessage) SerializationUtils.deserialize(data);
            return message;
        } else {
            if(data != null) {
                log.error("ERROR: Message digest("+ Arrays.toString(segmentHeader.digest)+") vs actual("+Arrays.toString(messageDigest.digest())+") did not match: " + new String(data, "UTF-8"));
            }
        }

        return null;
    }

    @PostConstruct
    public void init() {
        xStream = _xStream;
        gson = _gson;
    }


}
