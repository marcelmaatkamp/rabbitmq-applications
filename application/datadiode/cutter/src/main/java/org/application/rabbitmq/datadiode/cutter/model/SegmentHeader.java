package org.application.rabbitmq.datadiode.cutter.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import static org.application.rabbitmq.datadiode.cutter.model.SegmentType.*;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@XStreamAlias("segmentHeader")
public class SegmentHeader implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(SegmentHeader.class);


    private static final byte DIGEST_LENGTH = 32;

    public UUID uuid = UUID.randomUUID();

    public int size;
    public int blockSize;
    public int count;
    public Date insert = new Date();
    public Date update = new Date();
    public byte[] digest;

    public SegmentHeader size(final int size) {
        this.size = size;
        return this;
    }

    public SegmentHeader blockSize(final int blockSize) {
        this.blockSize = blockSize;
        return this;
    }

    public SegmentHeader count(int count) {
        this.count = count;
        return this;
    }

    public SegmentHeader digest(byte[] digest) {
        this.digest = digest;
        return this;
    }

    public SegmentHeader uuid(final UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public SegmentHeader insert(final Date insert) {
        this.insert = insert;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SegmentHeader that = (SegmentHeader) o;
        return uuid.equals(that.uuid);
    }

    /**
     * Gegerate a bytestream containing a SegmentHeader.
     *
     * @param doDigest indicates if digest should be calculated
     * @return
     */
    public byte[] toByteArray(boolean doDigest) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeByte(SEGMENT_HEADER.getType());
        oos.writeLong(uuid.getMostSignificantBits());
        oos.writeLong(uuid.getLeastSignificantBits());
        oos.writeInt(size);
        oos.writeInt(blockSize);
        oos.writeInt(count);
        oos.writeObject(insert);

        if(doDigest) {
            oos.writeInt(digest.length);
            oos.write(digest);
        }

        oos.close();
        bos.close();

        return bos.toByteArray();
    }

    public static SegmentHeader fromByteArray(byte[] segmentHeaderData, boolean doDigest) throws IOException, ClassNotFoundException {
        SegmentHeader segmentHeader = null;

        ByteArrayInputStream bis = new ByteArrayInputStream(segmentHeaderData);
        BufferedInputStream bus = new BufferedInputStream(bis);
        ObjectInputStream ois = new ObjectInputStream(bus);

        byte type = ois.readByte();
        if(type == SEGMENT_HEADER.getType()) {
            segmentHeader = fromByteArray(ois,segmentHeaderData,doDigest);
        } else {
            log.warn("this is not a segment header, type("+type+") unknown!");
        }

        ois.close();
        bus.close();
        bis.close();

        return segmentHeader;
    }

    public static SegmentHeader fromByteArray(ObjectInputStream ois, byte[] segmentHeaderData, boolean doDigest) throws IOException, ClassNotFoundException {
        SegmentHeader segmentHeader = new SegmentHeader();
        segmentHeader.
                uuid(new UUID(ois.readLong(), ois.readLong())).
                size(ois.readInt()).
                blockSize(ois.readInt()).
                count(ois.readInt()).
                insert((Date) ois.readObject());
        if (doDigest) {
            segmentHeader.digest(new byte[ois.readInt()]);
            ois.readFully(segmentHeader.digest);
        }
        return segmentHeader;
    }

}

