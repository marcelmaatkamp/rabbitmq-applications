package org.application.rabbitmq.datadiode.cutter.model;

/**
 * Created by marcel on 01-12-15.
 */
public enum SegmentType {

    SEGMENT_HEADER((byte)0),
    SEGMENT((byte)1);

    byte type;

    SegmentType(byte type) {
        this.type = type;
    }
}
