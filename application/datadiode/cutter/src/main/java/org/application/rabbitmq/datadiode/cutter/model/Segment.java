package org.application.rabbitmq.datadiode.cutter.model;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.codec.binary.Base64;
import org.application.rabbitmq.datadiode.cutter.util.MyByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * MTU 1500 bytes: 25 bytes header
 * MTU 9000 bytes: 25 bytes header
 * MTU 64k  bytes:
 * Created by marcelmaatkamp on 24/11/15.
 */

@XStreamAlias("segment")
public class Segment implements Serializable, Comparable<Segment> {

    private static final Logger log = LoggerFactory.getLogger(Segment.class);
    private static final int LONG_SIZE = 8;
    private static final int INT_SIZE = 4;
    public int index;
    public byte[] segment;
    public UUID uuid;
    public int count;

    public static Segment fromByteArray(byte[] segmentData) throws IOException {
        Segment segment = null;
        ByteBuffer b = ByteBuffer.wrap(segmentData);

        byte type = b.get();
        if (type == SegmentType.SEGMENT.getType()) {
            segment = fromByteArray(b, segmentData);
        } else {
            log.warn("This array is not a segment type(" + type + ") unknown!");
        }
/**
        ByteArrayInputStream bis = new ByteArrayInputStream(segmentData);
        byte type = (byte) bis.read();

        if (type == SegmentType.SEGMENT.getType()) {
            segment = fromByteArray(bis, segmentData);
        } else {
            log.warn("This array is not a segment type(" + type + ") unknown!");
        }
        bis.close();
   */

        return segment;
    }

    public static Segment fromByteArray(ByteBuffer b, byte[] segmentData) throws IOException {
        Segment segment = new Segment();
        // byte type = b.get();
        segment.uuid(new UUID(b.getLong(), b.getLong()));
        segment.count(b.getInt());
        segment.index(b.getInt());
        segment.segment = new byte[b.getInt()];
        b.get(segment.segment);
        return segment;

        /**
        byte[] longByteArray = new byte[LONG_SIZE];
        byte[] intByteArray = new byte[INT_SIZE];

        Segment segment = new Segment();
        bis.read(longByteArray);
        long most = Longs.fromByteArray(longByteArray);

        bis.read(longByteArray);
        segment.uuid(new UUID(most, Longs.fromByteArray(longByteArray)));

        bis.read(intByteArray);
        segment.count(Ints.fromByteArray(intByteArray));

        bis.read(intByteArray);
        segment.index(Ints.fromByteArray(intByteArray));

        bis.read(intByteArray);
        segment.segment = new byte[Ints.fromByteArray(intByteArray)];
        bis.read(segment.segment);
        return segment;
         */
    }

    public byte[] toByteArray() throws IOException {

        ByteBuffer bos = ByteBuffer.allocate(29 + segment.length);
        // 1 + 8 + 8 + 4 + 4 + 4 = 29 + segment.length
        bos.put(SegmentType.SEGMENT.getType());
        bos.putLong(uuid.getMostSignificantBits());
        bos.putLong(uuid.getLeastSignificantBits());
        bos.putInt(count);
        bos.putInt(index);
        bos.putInt(segment.length);
        bos.put(segment);
        return bos.array();

/**
 ByteArrayOutputStream bos = new ByteArrayOutputStream( 29 + segment.length );

 // 1 + 8 + 8 + 4 + 4 + 4 = 29 + segment.length
 bos.write(SegmentType.SEGMENT.getType());
 bos.write(Longs.toByteArray(uuid.getMostSignificantBits()));
 bos.write(Longs.toByteArray(uuid.getLeastSignificantBits()));
 bos.write(Ints.toByteArray(count));
 bos.write(Ints.toByteArray(index));
 bos.write(Ints.toByteArray(segment.length));
 bos.write(segment);
 bos.close();

 return bos.toByteArray();
 */
    }

    public Segment index(final int index) {
        this.index = index;
        return this;
    }

    public Segment segment(final byte[] segment) {
        this.segment = segment;
        return this;
    }

    public Segment uuid(final UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public Segment count(final int count) {
        this.count = count;
        return this;
    }

    @Override
    public int compareTo(Segment o) {
        return this.index - o.index;
    }

    public String getDigest() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(segment);
        return Base64.encodeBase64String(messageDigest.digest());
    }

    public String toString() {
        try {
            return ("[" + uuid + "]: count(" + count + ").index(" + index + ").payload(" + segment.length + "): " + getDigest() + ")");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}

