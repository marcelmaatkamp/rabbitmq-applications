package org.application.rabbitmq.stream.util;

import org.apache.commons.codec.binary.Base64;
import org.application.rabbitmq.stream.model.Segment;
import org.application.rabbitmq.stream.model.SegmentHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.utils.SerializationUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
public class StreamUtils {
    private static final Logger log = LoggerFactory.getLogger(StreamUtils.class);


    public static List<Message> cut(Message message, int bufSize) {
        List<Message> results = new ArrayList();

        byte[] messageBytes = SerializationUtils.serialize(message);

        int aantal = (int)(messageBytes.length / bufSize);
        int modulo = messageBytes.length % bufSize;

        SegmentHeader sh = new SegmentHeader().size(messageBytes.length).blockSize(bufSize).count(aantal);

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setReceivedRoutingKey(sh.uuid.toString());
        results.add(new Message(SerializationUtils.serialize(sh), messageProperties));


        // blocksize
        for(int i = 0; i < aantal; i++) {
            int start = i * bufSize;
            int stop = start + bufSize;
            Segment segment = new Segment().index(i).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, start,stop));
            messageProperties.getHeaders().put("index", i);
            results.add(new Message(SerializationUtils.serialize(segment), messageProperties));
        }

        // and the rest
        if(modulo>0) {
            int start = aantal*bufSize;
            int stop = modulo;
            Segment segment = new Segment().index(aantal).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, aantal*bufSize,aantal*bufSize+modulo));
            messageProperties.getHeaders().put("index", aantal);
            results.add(new Message(SerializationUtils.serialize(segment), messageProperties));
        }

        return results;
    }

    public static Message reconstruct(List<Message> messages) throws IOException {
        SegmentHeader segmentHeader = (SegmentHeader) SerializationUtils.deserialize(messages.get(0).getBody());

        byte[] buffer = new byte[segmentHeader.size];
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();

        for(int i = 1; i< messages.size(); i++) {
            Segment segment = (Segment) SerializationUtils.deserialize(messages.get(i).getBody());
            bos2.write(segment.segment);
        }

        bos2.close();
        Message message = (Message) SerializationUtils.deserialize(bos2.toByteArray());
        return message;
    }


}
