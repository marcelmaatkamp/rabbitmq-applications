package org.application.rabbitmq.datadiode.cutter.model;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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

    public int index;

    public byte[] segment;

    public UUID uuid;

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

    @Override
    public int compareTo(Segment o) {
        return this.index - o.index;
    }

    static final int LONG_SIZE=8;
    static final int INT_SIZE=4;

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(SegmentType.SEGMENT.getType());
        bos.write(Longs.toByteArray(uuid.getMostSignificantBits()));
        bos.write(Longs.toByteArray(uuid.getLeastSignificantBits()));
        bos.write(Ints.toByteArray(index));
        bos.write(Ints.toByteArray(segment.length));
        bos.write(segment);
        bos.close();
        return bos.toByteArray();
    }

    public static Segment fromByteArray(byte[] segmentData) throws IOException {
        Segment segment = new Segment();
        ByteArrayInputStream bis = new ByteArrayInputStream(segmentData);

        byte type = (byte)bis.read();

        if (type == SegmentType.SEGMENT.getType()) {
            segment = fromByteArray(bis, segmentData);
        } else {
            log.warn("This array is not a segment type(" + type + ") unknown!");
        }

        bis.close();
        return segment;
    }

    public static Segment fromByteArray(ByteArrayInputStream bis, byte[] segmentData) throws IOException {
        byte[] longByteArray = new byte[LONG_SIZE];
        byte[] intByteArray = new byte[INT_SIZE];

        Segment segment = new Segment();
        bis.read(longByteArray);
        long most = Longs.fromByteArray(longByteArray);

        bis.read(longByteArray);
        segment.uuid(new UUID(most, Longs.fromByteArray(longByteArray)));

        bis.read(intByteArray);
        segment.index(Ints.fromByteArray(intByteArray));

        bis.read(intByteArray);
        segment.segment = new byte[Ints.fromByteArray(intByteArray)];
        bis.read(segment.segment);
        return segment;
    }
}

