package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class OrderReplaceDecoderTest extends Assertions {

    private static final String REAL = "5503e400000d18c47161ba00000000000028810000000000004529000001f400058fd4";
    private final OrderReplaceDecoder decoder = new OrderReplaceDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('U');
        assertThat(decoder.stockLocate()).isEqualTo(996);
        assertThat(decoder.tracking()).isEqualTo(0);
        assertThat(decoder.timestamp()).isEqualTo(14400026141114L);
        assertThat(decoder.originalOrderRef()).isEqualTo(10369L);
        assertThat(decoder.newOrderRef()).isEqualTo(17705L);
        assertThat(decoder.shares()).isEqualTo(500L);
        assertThat(decoder.price()).isEqualTo(364500L);
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            OrderReplaceDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.originalOrderRef()).isEqualTo(-1L);
            assertThat(d.newOrderRef()).isEqualTo(-1L);
            assertThat(d.shares()).isEqualTo(4_294_967_295L);
            assertThat(d.price()).isEqualTo(4_294_967_295L);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(35).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'U');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.putLong(-1L);
        bb.putInt(0xFFFFFFFF);
        bb.putInt(0xFFFFFFFF);
        return bb.array();
    }
}
