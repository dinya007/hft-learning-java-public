package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class OrderExecutedWithPriceDecoderTest extends Assertions {

    private final OrderExecutedWithPriceDecoder decoder = new OrderExecutedWithPriceDecoder();

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            OrderExecutedWithPriceDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.orderRef()).isEqualTo(-1L);
            assertThat(d.executedShares()).isEqualTo(4_294_967_295L);
            assertThat(d.matchNumber()).isEqualTo(-1L);
            assertThat(d.printable()).isEqualTo((char) 0xFF);
            assertThat(d.price()).isEqualTo(4_294_967_295L);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'C');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.put((byte) 0xFF);
        bb.putInt(0xFFFFFFFF);
        return bb.array();
    }
}
