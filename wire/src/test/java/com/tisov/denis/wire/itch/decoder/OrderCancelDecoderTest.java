package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class OrderCancelDecoderTest extends Assertions {

    private static final String REAL = "581e5200000d18c59668e70000000000001360000000c8";
    private final OrderCancelDecoder decoder = new OrderCancelDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('X');
        assertThat(decoder.stockLocate()).isEqualTo(7762);
        assertThat(decoder.tracking()).isEqualTo(0);
        assertThat(decoder.timestamp()).isEqualTo(14400045344999L);
        assertThat(decoder.orderRef()).isEqualTo(4960L);
        assertThat(decoder.cancelledShares()).isEqualTo(200L);
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            OrderCancelDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.orderRef()).isEqualTo(-1L);
            assertThat(Long.toUnsignedString(d.orderRef())).isEqualTo("18446744073709551615");
            assertThat(d.cancelledShares()).isEqualTo(4_294_967_295L);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(23).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'X');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.putInt(0xFFFFFFFF);
        return bb.array();
    }
}
