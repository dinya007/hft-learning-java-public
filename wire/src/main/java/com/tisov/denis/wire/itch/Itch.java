package com.tisov.denis.wire.itch;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;
import static java.nio.ByteOrder.BIG_ENDIAN;

public final class Itch {

    private Itch() {
    }

    public static final char SYSTEM_EVENT         = 'S';
    public static final char STOCK_DIRECTORY      = 'R';
    public static final char ADD_ORDER            = 'A';
    public static final char ADD_ORDER_MPID       = 'F';
    public static final char ORDER_EXECUTED       = 'E';
    public static final char ORDER_EXECUTED_PRICE = 'C';
    public static final char ORDER_CANCEL         = 'X';
    public static final char ORDER_DELETE         = 'D';
    public static final char ORDER_REPLACE        = 'U';
    public static final char TRADE                = 'P';
    public static final char STOCK_TRADING_ACTION = 'H';
    public static final char REG_SHO              = 'Y';
    public static final char MARKET_PARTICIPANT   = 'L';
    public static final char MWCB_DECLINE         = 'V';
    public static final char MWCB_STATUS          = 'W';
    public static final char IPO_QUOTING          = 'K';
    public static final char LULD_AUCTION_COLLAR  = 'J';
    public static final char OPERATIONAL_HALT     = 'h';
    public static final char CROSS_TRADE          = 'Q';
    public static final char BROKEN_TRADE         = 'B';
    public static final char NOII                 = 'I';
    public static final char RPII                 = 'N';

    public static final int LEN_SYSTEM_EVENT         = 12;
    public static final int LEN_STOCK_DIRECTORY      = 39;
    public static final int LEN_ADD_ORDER            = 36;
    public static final int LEN_ADD_ORDER_MPID       = 40;
    public static final int LEN_ORDER_EXECUTED       = 31;
    public static final int LEN_ORDER_EXECUTED_PRICE = 36;
    public static final int LEN_ORDER_CANCEL         = 23;
    public static final int LEN_ORDER_DELETE         = 19;
    public static final int LEN_ORDER_REPLACE        = 35;
    public static final int LEN_TRADE                = 44;
    public static final int LEN_STOCK_TRADING_ACTION = 25;
    public static final int LEN_REG_SHO              = 20;
    public static final int LEN_MARKET_PARTICIPANT   = 26;
    public static final int LEN_MWCB_DECLINE         = 35;
    public static final int LEN_MWCB_STATUS          = 12;
    public static final int LEN_IPO_QUOTING          = 28;
    public static final int LEN_LULD_AUCTION_COLLAR  = 35;
    public static final int LEN_OPERATIONAL_HALT     = 21;
    public static final int LEN_CROSS_TRADE          = 40;
    public static final int LEN_BROKEN_TRADE         = 19;
    public static final int LEN_NOII                 = 50;
    public static final int LEN_RPII                 = 20;

    private static final int[] MSG_LEN = new int[256];
    static {
        MSG_LEN[SYSTEM_EVENT]         = LEN_SYSTEM_EVENT;
        MSG_LEN[STOCK_DIRECTORY]      = LEN_STOCK_DIRECTORY;
        MSG_LEN[STOCK_TRADING_ACTION] = LEN_STOCK_TRADING_ACTION;
        MSG_LEN[REG_SHO]              = LEN_REG_SHO;
        MSG_LEN[MARKET_PARTICIPANT]   = LEN_MARKET_PARTICIPANT;
        MSG_LEN[MWCB_DECLINE]         = LEN_MWCB_DECLINE;
        MSG_LEN[MWCB_STATUS]          = LEN_MWCB_STATUS;
        MSG_LEN[IPO_QUOTING]          = LEN_IPO_QUOTING;
        MSG_LEN[LULD_AUCTION_COLLAR]  = LEN_LULD_AUCTION_COLLAR;
        MSG_LEN[OPERATIONAL_HALT]     = LEN_OPERATIONAL_HALT;
        MSG_LEN[ADD_ORDER]            = LEN_ADD_ORDER;
        MSG_LEN[ADD_ORDER_MPID]       = LEN_ADD_ORDER_MPID;
        MSG_LEN[ORDER_EXECUTED]       = LEN_ORDER_EXECUTED;
        MSG_LEN[ORDER_EXECUTED_PRICE] = LEN_ORDER_EXECUTED_PRICE;
        MSG_LEN[ORDER_CANCEL]         = LEN_ORDER_CANCEL;
        MSG_LEN[ORDER_DELETE]         = LEN_ORDER_DELETE;
        MSG_LEN[ORDER_REPLACE]        = LEN_ORDER_REPLACE;
        MSG_LEN[TRADE]                = LEN_TRADE;
        MSG_LEN[CROSS_TRADE]          = LEN_CROSS_TRADE;
        MSG_LEN[BROKEN_TRADE]         = LEN_BROKEN_TRADE;
        MSG_LEN[NOII]                 = LEN_NOII;
        MSG_LEN[RPII]                 = LEN_RPII;
    }

    public static int messageLength(char type) {
        return MSG_LEN[type & 0xFF];
    }

    public static final long PRICE_SCALE = 10_000L;

    private static final ValueLayout.OfShort U16_BE = JAVA_SHORT_UNALIGNED.withOrder(BIG_ENDIAN);
    private static final ValueLayout.OfInt   U32_BE = JAVA_INT_UNALIGNED.withOrder(BIG_ENDIAN);
    private static final ValueLayout.OfLong  U64_BE = JAVA_LONG_UNALIGNED.withOrder(BIG_ENDIAN);

    private static final int UNSIGNED_BYTE_MASK = 0xFF;
    private static final int UNSIGNED_SHORT_MASK = 0xFFFF;
    private static final long UNSIGNED_SHORT_MASK_L = 0xFFFFL;
    private static final long UNSIGNED_INT_MASK = 0xFFFF_FFFFL;

    public static char readChar(MemorySegment seg, long offset) {
        return (char) readU8(seg, offset);
    }

    public static int readU8(MemorySegment seg, long offset) {
        return seg.get(JAVA_BYTE, offset) & UNSIGNED_BYTE_MASK;
    }

    public static int readU16BE(MemorySegment seg, long offset) {
        return seg.get(U16_BE, offset) & UNSIGNED_SHORT_MASK;
    }

    public static long readU32BE(MemorySegment seg, long offset) {
        return seg.get(U32_BE, offset) & UNSIGNED_INT_MASK;
    }

    public static long readU48BE(MemorySegment seg, long offset) {
        return ((seg.get(U16_BE, offset) & UNSIGNED_SHORT_MASK_L) << 32)
               | (seg.get(U32_BE, offset + 2) & UNSIGNED_INT_MASK);
    }

    public static long readU64BE(MemorySegment seg, long offset) {
        return seg.get(U64_BE, offset);
    }

    public static long readSymbol(MemorySegment seg, long offset) {
        return readU64BE(seg, offset);
    }
}
