package org.application.rabbitmq.datadiode.cutter.model;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by marcelmaatkamp on 02/12/15.
 */
public class SegmentHeaderTest {
    private static final Logger log = LoggerFactory.getLogger(SegmentHeaderTest.class);

    final int SEGMENT_HEADER_WITH_DIGEST_SIZE = 115; // 111 bytes
    final int SEGMENT_HEADER_WITHOUT_DIGEST_SIZE = 77; // 77 bytes

    @Test
    public void testToAndFromByteArrayWithDigest() throws Exception {
        byte[] randomBytes = RandomUtils.nextBytes(65535);
        Date now = new Date();

        boolean calculateDigest = true;

        SegmentHeader segmentHeader = new SegmentHeader().
                uuid(UUID.randomUUID()).
                size(15).blockSize(16).count(17).insert(now);

        if(calculateDigest) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(randomBytes);
            segmentHeader.digest(md.digest());
        }

        byte[] segmentHeaderData = segmentHeader.toByteArray(calculateDigest);
        assertEquals(segmentHeaderData.length, SEGMENT_HEADER_WITH_DIGEST_SIZE);

        SegmentHeader other = SegmentHeader.fromByteArray(segmentHeaderData, calculateDigest);

        assertEquals(segmentHeader.uuid, other.uuid);
        assertEquals(segmentHeader.size, other.size);
        assertEquals(segmentHeader.blockSize, other.blockSize);
        assertEquals(segmentHeader.count, other.count);
        assertEquals(segmentHeader.insert, other.insert);
        assertArrayEquals(segmentHeader.digest, other.digest);
    }

    @Test
    public void testToAndFromByteArrayWithoutDigest() throws Exception {

        byte[] randomBytes = RandomUtils.nextBytes(65535);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(randomBytes);

        Date now = new Date();
        boolean calculateDigest = false;

        SegmentHeader segmentHeader = new SegmentHeader().
                uuid(UUID.randomUUID()).
                size(15).blockSize(16).count(17).insert(now);

        byte[] segmentHeaderData = segmentHeader.toByteArray(calculateDigest);
        assertEquals(segmentHeaderData.length, SEGMENT_HEADER_WITHOUT_DIGEST_SIZE);

        SegmentHeader other = SegmentHeader.fromByteArray(segmentHeaderData, calculateDigest);

        assertEquals(segmentHeader.uuid, other.uuid);
        assertEquals(segmentHeader.size, other.size);
        assertEquals(segmentHeader.blockSize, other.blockSize);
        assertEquals(segmentHeader.count, other.count);
        assertEquals(segmentHeader.insert, other.insert);
        assertNull(other.digest);
    }

    @Test
    public void testSegmentByteArray() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        byte[] segment =  RandomUtils.nextBytes(1500);
        int index = 10;
        UUID uuid = UUID.randomUUID();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeByte(SegmentType.SEGMENT.getType());
        oos.writeLong(uuid.getMostSignificantBits());
        oos.writeLong(uuid.getLeastSignificantBits());
        oos.writeInt(index);
        oos.writeInt(segment.length);
        oos.write(segment);
        oos.flush();

        oos.close();
        bos.close();

        byte[] result = bos.toByteArray();
        log.info("length("+result.length+"), data: " + Hex.encodeHexString( result ) );

        ByteArrayInputStream bis = new ByteArrayInputStream(result);
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));

        Assert.assertEquals(SegmentType.SEGMENT.getType(),ois.readByte());
        Assert.assertTrue(uuid.equals(new UUID(ois.readLong(), ois.readLong())));
        Assert.assertEquals(index, ois.readInt());
        byte[] other_segment = new byte[ois.readInt()];
        Assert.assertEquals(segment.length, other_segment.length);
        ois.readFully(other_segment);
        Assert.assertArrayEquals(segment, other_segment);

        log.info("header.size("+(result.length-segment.length)+")"); //  1500, 39 bytes header
        log.info("total.size("+result.length+")");

        bis.close();
        ois.close();
    }

}
