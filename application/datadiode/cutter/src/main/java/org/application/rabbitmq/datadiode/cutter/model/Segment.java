package org.application.rabbitmq.datadiode.cutter.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */

@XStreamAlias("segment")
public class Segment implements Serializable, Comparable<Segment> {

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
}
