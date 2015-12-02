package org.application.rabbitmq.datadiode.cutter.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.UUID;

/**
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


    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeByte(SegmentType.SEGMENT.getType());
        oos.writeLong(this.uuid.getMostSignificantBits());
        oos.writeLong(this.uuid.getLeastSignificantBits());
        oos.writeInt(this.index);
        oos.writeInt(this.segment.length);
        oos.write(this.segment);

        oos.close();
        bos.close();

        return bos.toByteArray();
    }

    public static Segment fromByteArray(byte[] segmentData) throws IOException {
        Segment segment = new Segment();
        ByteArrayInputStream bis = new ByteArrayInputStream(segmentData);
        BufferedInputStream bus = new BufferedInputStream(bis);
        ObjectInputStream ois = new ObjectInputStream(bus);
        byte type = ois.readByte();

        if (type == SegmentType.SEGMENT.getType()) {
            fromByteArray(ois, segmentData);
        } else {
            log.warn("This array is not a segment type(" + type + ") unknown!");
        }

        ois.close();
        bus.close();
        bis.close();
        return segment;
    }

    public static Segment fromByteArray(ObjectInputStream ois, byte[] segmentData) throws IOException {
        Segment segment = new Segment();
        segment.uuid(new UUID(ois.readLong(), ois.readLong()));
        segment.index(ois.readInt());
        segment.segment = new byte[ois.readInt()];
        ois.readFully(segment.segment);
        return segment;
    }
}

