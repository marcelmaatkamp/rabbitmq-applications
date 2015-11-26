package org.application.rabbitmq.stream.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
public class SegmentHeader implements Serializable {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SegmentHeader that = (SegmentHeader) o;
        return uuid.equals(that.uuid);
    }
}
