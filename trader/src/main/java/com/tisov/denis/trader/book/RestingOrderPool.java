package com.tisov.denis.trader.book;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public final class RestingOrderPool {

    private static final int ALIGNMENT = 64;
    private static final int SLOT_SIZE = 64;
    private static final int MIN_CAPACITY = 64;
    private static final int MASK = 63;

    private static final int OFF_ORDER_REF = 0;
    private static final int OFF_PRICE = 8;
    private static final int OFF_SHARES = 16;
    private static final int OFF_TIMESTAMP = 24;
    private static final int OFF_STOCK = 32;
    private static final int OFF_LOCATE = 40;
    private static final int OFF_SIDE = 44;

    private final MemorySegment memorySegment;
    private final int capacity;
    private final long[] freeBits;

    private int searchHint;
    private int inUse;
    private int peakInUse;

    public RestingOrderPool(int capacity) {
        if (capacity < MIN_CAPACITY) {
            throw new IllegalArgumentException(
                    "capacity must be >= %d, got %d".formatted(MIN_CAPACITY, capacity));
        }
        if (Integer.bitCount(capacity) != 1) {
            int next = Integer.highestOneBit(capacity) << 1;
            throw new IllegalArgumentException(
                    "capacity must be a power of 2, got %d (next valid: %d)".formatted(capacity, next));
        }

        this.memorySegment = Arena.global().allocate((long) capacity * SLOT_SIZE, ALIGNMENT);
        this.capacity = capacity;

        int words = capacity >>> 6;
        this.freeBits = new long[words];
        for (int w = 0; w < words; w++) {
            this.freeBits[w] = -1L;
        }
        this.searchHint = 0;
    }

    public int acquire() {
        for (int w = this.searchHint; w < this.freeBits.length; w++) {
            long word = freeBits[w];
            if (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                this.freeBits[w] &= ~(1L << bit);
                ++inUse;
                if (inUse > peakInUse) {
                    peakInUse = inUse;
                }
                searchHint = w;
                return (w << 6) | bit;
            }
        }

        throw new IllegalStateException(
                "RestingOrderPool exhausted: capacity=%d, inUse=%d — undersized or leak (book not releasing on Delete)"
                        .formatted(capacity, inUse)
        );
    }

    public void release(int index) {
        int word = index >>> 6;
        this.freeBits[word] |= 1L << (index & MASK);
        --inUse;
        if (word < searchHint) {
            searchHint = word;
        }
    }

    public int inUse() {
        return inUse;
    }

    public int peakInUse() {
        return peakInUse;
    }

    public int capacity() {
        return capacity;
    }

    public void set(int index, long orderRef, long price, long shares, long timestamp, long stock, int locate, byte side) {
        long base = slotBase(index);
        memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, base + OFF_ORDER_REF, orderRef);
        memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, base + OFF_PRICE, price);
        memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, base + OFF_SHARES, shares);
        memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, base + OFF_TIMESTAMP, timestamp);
        memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, base + OFF_STOCK, stock);
        memorySegment.set(ValueLayout.JAVA_INT_UNALIGNED, base + OFF_LOCATE, locate);
        memorySegment.set(ValueLayout.JAVA_BYTE, base + OFF_SIDE, side);
    }

    public long reduceShares(int index, long delta) {
        long sharesOffset = slotBase(index) + OFF_SHARES;
        long remainingShares = memorySegment.get(ValueLayout.JAVA_LONG_UNALIGNED, sharesOffset) - delta;
        assert remainingShares >= 0 : "Negative amount of shares after execution %s";

        memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, sharesOffset, remainingShares);

        return remainingShares;
    }

    public long orderRef(int index) {
        return memorySegment.get(ValueLayout.JAVA_LONG_UNALIGNED, slotBase(index) + OFF_ORDER_REF);
    }

    public long price(int index) {
        return memorySegment.get(ValueLayout.JAVA_LONG_UNALIGNED, slotBase(index) + OFF_PRICE);
    }

    public long shares(int index) {
        return memorySegment.get(ValueLayout.JAVA_LONG_UNALIGNED, slotBase(index) + OFF_SHARES);
    }

    public long timestamp(int index) {
        return memorySegment.get(ValueLayout.JAVA_LONG_UNALIGNED, slotBase(index) + OFF_TIMESTAMP);
    }

    public long stock(int index) {
        return memorySegment.get(ValueLayout.JAVA_LONG_UNALIGNED, slotBase(index) + OFF_STOCK);
    }

    public int locate(int index) {
        return memorySegment.get(ValueLayout.JAVA_INT_UNALIGNED, slotBase(index) + OFF_LOCATE);
    }

    public byte side(int index) {
        return memorySegment.get(ValueLayout.JAVA_BYTE, slotBase(index) + OFF_SIDE);
    }

    private static long slotBase(int index) {
        return (long) index * SLOT_SIZE;
    }

}
