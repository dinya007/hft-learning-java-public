package com.tisov.denis.wire.itch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;

import static com.tisov.denis.wire.itch.TestUtils.SAMPLE_FILE;

class ItchFeedIntegrityWalkTest extends Assertions {

    @Test
    void ourMessageLengthsMatchTheRealFeed() throws IOException {
        MemorySegment seg = MemorySegment.ofArray(Files.readAllBytes(SAMPLE_FILE));
        long size = seg.byteSize();

        long off = 0;
        int messages = 0;
        while (off + 2 <= size) {
            int len = Itch.readU16BE(seg, off);
            if (off + 2 + len > size) {
                fail("frame at offset %d (len %d) overruns file size %d".formatted(off, len, size));
            }

            int type = Itch.readU8(seg, off + 2);
            int expected = expectedLength(type);
            if (expected != -1 && len != expected) {
                fail("type '%c' wire length %d != our LEN constant %d".formatted((char) type, len, expected));
            }

            off += 2 + len;
            messages++;
        }

        assertThat(off).as("walk must consume the file exactly").isEqualTo(size);
        assertThat(messages).as("message count").isPositive();
    }

    private static int expectedLength(int type) {
        return switch (type) {
            case 'S' -> Itch.LEN_SYSTEM_EVENT;
            case 'R' -> Itch.LEN_STOCK_DIRECTORY;
            case 'A' -> Itch.LEN_ADD_ORDER;
            case 'F' -> Itch.LEN_ADD_ORDER_MPID;
            case 'E' -> Itch.LEN_ORDER_EXECUTED;
            case 'C' -> Itch.LEN_ORDER_EXECUTED_PRICE;
            case 'X' -> Itch.LEN_ORDER_CANCEL;
            case 'D' -> Itch.LEN_ORDER_DELETE;
            case 'U' -> Itch.LEN_ORDER_REPLACE;
            case 'P' -> Itch.LEN_TRADE;
            default -> -1;
        };
    }

}
