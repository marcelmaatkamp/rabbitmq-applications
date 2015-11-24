package org.application.rabbitmq.stream.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
public class Segment implements Serializable {
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
}
