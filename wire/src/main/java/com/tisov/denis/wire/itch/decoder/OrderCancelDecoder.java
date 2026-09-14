package com.tisov.denis.wire.itch.decoder;

import com.tisov.denis.wire.itch.Itch;

import java.lang.foreign.MemorySegment;

public final class OrderCancelDecoder {

    private static final int OFF_MESSAGE_TYPE = 0;
    private static final int OFF_STOCK_LOCATE = 1;
    private static final int OFF_TRACKING = 3;
    private static final int OFF_TIMESTAMP = 5;
    private static final int OFF_ORDER_REF = 11;
    private static final int OFF_CANCELLED_SHARES = 19;

    private MemorySegment memorySegment;
    private long base;

    public OrderCancelDecoder wrap(MemorySegment memorySegment, long base) {
        this.memorySegment = memorySegment;
        this.base = base;
        return this;
    }

    public char messageType() {
        return Itch.readChar(memorySegment, base + OFF_MESSAGE_TYPE);
    }

    public int stockLocate() {
        return Itch.readU16BE(memorySegment, base + OFF_STOCK_LOCATE);
    }

    public int tracking() {
        return Itch.readU16BE(memorySegment, base + OFF_TRACKING);
    }

    public long timestamp() {
        return Itch.readU48BE(memorySegment, base + OFF_TIMESTAMP);
    }

    public long orderRef() {
        return Itch.readU64BE(memorySegment, base + OFF_ORDER_REF);
    }

    public long cancelledShares() {
        return Itch.readU32BE(memorySegment, base + OFF_CANCELLED_SHARES);
    }
}
