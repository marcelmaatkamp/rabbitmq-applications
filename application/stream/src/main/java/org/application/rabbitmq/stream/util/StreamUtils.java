package org.application.rabbitmq.stream.util;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.codec.binary.Base64;
import org.application.rabbitmq.stream.model.Segment;
import org.application.rabbitmq.stream.model.SegmentHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.*;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
public class StreamUtils {
    private static final Logger log = LoggerFactory.getLogger(StreamUtils.class);

    @Autowired
    XStream xStream;

    public static List<Message> cut(Message message, int bufSize) {
        List<Message> results = new ArrayList();

        byte[] messageBytes = SerializationUtils.serialize(message);

        int aantal = (int)(messageBytes.length / bufSize);
        int modulo = messageBytes.length % bufSize;

        SegmentHeader sh = new SegmentHeader().size(messageBytes.length).blockSize(bufSize).count(aantal);

        MessageProperties messageProperties = new MessageProperties();
        // messageProperties.setReceivedRoutingKey(sh.uuid.toString());
        messageProperties.getHeaders().put("type", sh.getClass());
        messageProperties.getHeaders().put("uuid", sh.uuid);
        messageProperties.getHeaders().put("block", sh.blockSize);
        messageProperties.getHeaders().put("count", sh.count);
        messageProperties.getHeaders().put("size", sh.size);

        results.add(new Message(SerializationUtils.serialize(sh), messageProperties));

        // blocksize
        for(int i = 0; i < aantal; i++) {
            int start = i * bufSize;
            int stop = start + bufSize;
            Segment segment = new Segment().index(i).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, start,stop));
            messageProperties = new MessageProperties();
            messageProperties.getHeaders().put("type", segment.getClass());
            messageProperties.getHeaders().put("uuid", segment.uuid);
            messageProperties.getHeaders().put("index", segment.index);
            messageProperties.getHeaders().put("count", sh.count);
            messageProperties.getHeaders().put("size", segment.segment.length);
            results.add(new Message(SerializationUtils.serialize(segment), messageProperties));
        }

        // and the rest
        if(modulo>0) {
            int start = aantal*bufSize;
            int stop = modulo;
            Segment segment = new Segment().index(aantal).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, aantal*bufSize,aantal*bufSize+modulo));
            messageProperties = new MessageProperties();
            messageProperties.getHeaders().put("type", segment.getClass());
            messageProperties.getHeaders().put("uuid", segment.uuid);
            messageProperties.getHeaders().put("index", segment.index);
            messageProperties.getHeaders().put("count", sh.count);
            messageProperties.getHeaders().put("size", segment.segment.length);
            results.add(new Message(SerializationUtils.serialize(segment), messageProperties));
        }

        return results;
    }

    public static Message reconstruct(List<Message> messages) throws IOException {
        SegmentHeader segmentHeader = (SegmentHeader) SerializationUtils.deserialize(messages.get(0).getBody());

        byte[] buffer = new byte[segmentHeader.size];
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();

        List<Segment> sortedArrayList = new ArrayList();

        for(int i = 1; i< messages.size(); i++) {
            Segment segment = (Segment) SerializationUtils.deserialize(messages.get(i).getBody());
            // out-of-order: sort
            sortedArrayList.add(segment);
        }

        Collections.sort(sortedArrayList);

        for(Segment segment : sortedArrayList) {
            bos2.write(segment.segment);
        }

        bos2.close();
        Message message = (Message) SerializationUtils.deserialize(bos2.toByteArray());
        return message;
    }



}
