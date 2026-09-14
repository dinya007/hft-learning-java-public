package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class OrderExecutedDecoderTest extends Assertions {

    private static final String REAL = "450ab500020d18c3b04daf00000000000009c6000000640000000000004594";
    private final OrderExecutedDecoder decoder = new OrderExecutedDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('E');
        assertThat(decoder.stockLocate()).isEqualTo(2741);
        assertThat(decoder.tracking()).isEqualTo(2);
        assertThat(decoder.timestamp()).isEqualTo(14400013487535L);
        assertThat(decoder.orderRef()).isEqualTo(2502L);
        assertThat(decoder.executedShares()).isEqualTo(100L);
        assertThat(decoder.matchNumber()).isEqualTo(17812L);
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            OrderExecutedDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.orderRef()).isEqualTo(-1L);
            assertThat(Long.toUnsignedString(d.orderRef())).isEqualTo("18446744073709551615");
            assertThat(d.executedShares()).isEqualTo(4_294_967_295L);
            assertThat(d.matchNumber()).isEqualTo(-1L);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(31).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'E');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        return bb.array();
    }
}
