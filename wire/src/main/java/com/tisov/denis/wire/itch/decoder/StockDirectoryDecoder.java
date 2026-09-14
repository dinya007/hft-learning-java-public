package com.tisov.denis.wire.itch.decoder;

import com.tisov.denis.wire.itch.Itch;

import java.lang.foreign.MemorySegment;

public final class StockDirectoryDecoder {

    private static final int OFF_MESSAGE_TYPE = 0;
    private static final int OFF_STOCK_LOCATE = 1;
    private static final int OFF_TRACKING = 3;
    private static final int OFF_TIMESTAMP = 5;
    private static final int OFF_STOCK = 11;
    private static final int OFF_MARKET_CATEGORY = 19;
    private static final int OFF_FINANCIAL_STATUS = 20;
    private static final int OFF_ROUND_LOT_SIZE = 21;
    private static final int OFF_ROUND_LOTS_ONLY = 25;
    private static final int OFF_ISSUE_CLASSIFICATION = 26;
    private static final int OFF_ISSUE_SUBTYPE = 27;
    private static final int OFF_AUTHENTICITY = 29;
    private static final int OFF_SHORT_SALE_THRESHOLD = 30;
    private static final int OFF_IPO_FLAG = 31;
    private static final int OFF_LULD_TIER = 32;
    private static final int OFF_ETP_FLAG = 33;
    private static final int OFF_ETP_LEVERAGE = 34;
    private static final int OFF_INVERSE_INDICATOR = 38;

    private MemorySegment memorySegment;
    private long base;

    public StockDirectoryDecoder wrap(MemorySegment memorySegment, long base) {
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

    public long stock() {
        return Itch.readU64BE(memorySegment, base + OFF_STOCK);
    }

    public char marketCategory() {
        return Itch.readChar(memorySegment, base + OFF_MARKET_CATEGORY);
    }

    public char financialStatus() {
        return Itch.readChar(memorySegment, base + OFF_FINANCIAL_STATUS);
    }

    public long roundLotSize() {
        return Itch.readU32BE(memorySegment, base + OFF_ROUND_LOT_SIZE);
    }

    public char roundLotsOnly() {
        return Itch.readChar(memorySegment, base + OFF_ROUND_LOTS_ONLY);
    }

    public char issueClassification() {
        return Itch.readChar(memorySegment, base + OFF_ISSUE_CLASSIFICATION);
    }

    public int issueSubType() {
        return Itch.readU16BE(memorySegment, base + OFF_ISSUE_SUBTYPE);
    }

    public char authenticity() {
        return Itch.readChar(memorySegment, base + OFF_AUTHENTICITY);
    }

    public char shortSaleThreshold() {
        return Itch.readChar(memorySegment, base + OFF_SHORT_SALE_THRESHOLD);
    }

    public char ipoFlag() {
        return Itch.readChar(memorySegment, base + OFF_IPO_FLAG);
    }

    public char luldReferencePriceTier() {
        return Itch.readChar(memorySegment, base + OFF_LULD_TIER);
    }

    public char etpFlag() {
        return Itch.readChar(memorySegment, base + OFF_ETP_FLAG);
    }

    public long etpLeverageFactor() {
        return Itch.readU32BE(memorySegment, base + OFF_ETP_LEVERAGE);
    }

    public char inverseIndicator() {
        return Itch.readChar(memorySegment, base + OFF_INVERSE_INDICATOR);
    }
}
