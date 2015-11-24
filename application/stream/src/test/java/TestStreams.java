import junit.framework.Assert;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.*;
import org.apache.commons.lang3.RandomUtils;
import org.application.rabbitmq.stream.model.Segment;
import org.application.rabbitmq.stream.model.SegmentHeader;
import org.application.rabbitmq.stream.util.StreamUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.utils.SerializationUtils;

import java.io.*;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */

public class TestStreams implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(TestStreams.class);

    @Test
    public void testStream() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);

        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(new Message("hello".getBytes(), new MessageProperties()));
        oos.writeObject(new Message("hi".getBytes(), new MessageProperties()));
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        log.info(((Message) ois.readObject()).toString());
        log.info(((Message) ois.readObject()).toString());
        ois.close();

    }

    @Test
    public void testBigStream() throws IOException, ClassNotFoundException {

        Message bigMessage = new Message(new byte[65535], new MessageProperties());

        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(bigMessage);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        log.info(String.valueOf((Message) ois.readObject()));
        ois.close();
        bos.close();

    }

    @Test
    public void testSegmentedStream() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        int length = 90;
        int bufSize = 1500;

        byte[] randomBytes = RandomUtils.nextBytes(length);
        Message message = new Message(randomBytes, new MessageProperties());

        byte[] messageBytes = SerializationUtils.serialize(message);
        md.update(messageBytes);
        String digest = Base64.encodeBase64String(md.digest());

        ByteArrayOutputStream bos = new ByteArrayOutputStream(bufSize);
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        SegmentHeader sh = new SegmentHeader().size(messageBytes.length).blockSize(bufSize);
        oos.writeObject(sh);
        log.info("header.size: " + SerializationUtils.serialize(sh).length);

        int aantal = (int)(messageBytes.length / bufSize);
        int modulo = messageBytes.length % bufSize;
        log.info("lenght: " + messageBytes.length +", aantal: " + aantal + ", mod: " + modulo);

        // blocksize
        for(int i = 0; i < aantal; i++) {
            int start = i * bufSize;
            int stop = start + bufSize;
            log.info("> ["+i+"]: start("+start+"), stop("+stop+")");
            Segment segment = new Segment().index(i).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, start,stop));
            oos.writeObject(segment);
        }

        // and the rest
        if(modulo>0) {
            int start = aantal*bufSize;
            int stop = modulo;
            log.info("> ["+aantal+"]: " + aantal + ", start("+start+"), stop("+stop+")");
            Segment segment = new Segment().index(aantal).uuid(sh.uuid).segment(Arrays.copyOfRange(messageBytes, aantal*bufSize,aantal*bufSize+modulo));
            oos.writeObject(segment);
        }
        oos.close();


        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        SegmentHeader segmentHeader = (SegmentHeader) ois.readObject();

        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();

        for(int i = 0; i< segmentHeader.size / segmentHeader.blockSize; i++) {
            Segment segment = (Segment)ois.readObject();
            bos2.write(segment.segment);

            log.info("< ["+segment.uuid+"]["+segment.index+"]: " + segment.segment.length);
        }
        if(segmentHeader.size % segmentHeader.blockSize > 0) {
            Segment segment = (Segment)ois.readObject();
            bos2.write(segment.segment);
            log.info("< ["+segment.uuid+"]["+segment.index+"]: " + segment.segment.length);
        }
        ois.close();
        bos.close();

        md.update(bos2.toByteArray());
        String digest2 = Base64.encodeBase64String(md.digest());
        org.junit.Assert.assertEquals(digest,digest2);

        Message message1 = (Message) SerializationUtils.deserialize(bos2.toByteArray());
        org.junit.Assert.assertArrayEquals(message.getBody(), message1.getBody());

    }

    // Implementing Fisher–Yates shuffle
    static void shuffleArray(int[] ar)
    {
        // If running on Java 6 or older, use `new Random()` on RHS here
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    @Test
    public void testStreams() throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        int length = 90000;
        int bufSize = 1500;

        byte[] randomBytes = RandomUtils.nextBytes(length);
        Message message = new Message(randomBytes, new MessageProperties());
        md.update(randomBytes);
        String digest = Base64.encodeBase64String(md.digest());

        List<Message> messages = StreamUtils.cut(message, bufSize);
        Message cutMessage = StreamUtils.reconstruct(messages);

        md.update(cutMessage.getBody());
        String cutDigest = Base64.encodeBase64String(md.digest());

        org.junit.Assert.assertEquals(digest,cutDigest);
    }
}