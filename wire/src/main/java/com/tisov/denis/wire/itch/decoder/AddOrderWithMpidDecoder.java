package com.tisov.denis.wire.itch.decoder;

import com.tisov.denis.wire.itch.Itch;

import java.lang.foreign.MemorySegment;

public final class AddOrderWithMpidDecoder {

    private static final int OFF_MESSAGE_TYPE = 0;
    private static final int OFF_STOCK_LOCATE = 1;
    private static final int OFF_TRACKING = 3;
    private static final int OFF_TIMESTAMP = 5;
    private static final int OFF_ORDER_REF = 11;
    private static final int OFF_SIDE = 19;
    private static final int OFF_SHARES = 20;
    private static final int OFF_STOCK = 24;
    private static final int OFF_PRICE = 32;
    private static final int OFF_ATTRIBUTION = 36;

    private MemorySegment memorySegment;
    private long base;

    public AddOrderWithMpidDecoder wrap(MemorySegment memorySegment, long base) {
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

    public char side() {
        return Itch.readChar(memorySegment, base + OFF_SIDE);
    }

    public long shares() {
        return Itch.readU32BE(memorySegment, base + OFF_SHARES);
    }

    public long stock() {
        return Itch.readU64BE(memorySegment, base + OFF_STOCK);
    }

    public long price() {
        return Itch.readU32BE(memorySegment, base + OFF_PRICE);
    }

    public long attribution() {
        return Itch.readU32BE(memorySegment, base + OFF_ATTRIBUTION);
    }
}
