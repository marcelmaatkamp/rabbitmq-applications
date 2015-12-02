package org.application.rabbitmq.datadiode.cutter.model;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by marcelmaatkamp on 02/12/15.
 */
public class SegmentTest {
    private static final Logger log = LoggerFactory.getLogger(SegmentTest.class);


    int SEGMENT_SIZE=65536;
    int SEGMENT_MTU_SIZE=1500;


    @Test
    public void testSegmentByteArray() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        byte[] segment =  RandomUtils.nextBytes(1255);
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

        // 0:31, 1:31,
        log.info("header.size("+(result.length-segment.length)+")"); //  0..230: 31, 231..999: 34, 1000..1254: 36, 1255..(1500)..: 39, 2048: 41, 64k: 351 bytes header
        log.info("total.size("+result.length+")");

        bis.close();
        ois.close();
    }

    @Test
    public void testToAndFromByteArray() throws IOException {
        Segment segment = new Segment().uuid(UUID.randomUUID()).index(10).segment(RandomUtils.nextBytes(SEGMENT_SIZE));

        byte[] array = segment.toByteArray();
        Segment other = Segment.fromByteArray(array);

        assertEquals(segment.uuid, other.uuid);
        assertEquals(segment.index, other.index);
        assertArrayEquals(segment.segment, other.segment);
    }

}
