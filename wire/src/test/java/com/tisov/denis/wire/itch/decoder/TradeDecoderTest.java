package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class TradeDecoderTest extends Assertions {

    private static final String REAL = "5012bd00020d19c4f36f5d000000000000000042000000324c494e2020202020001f8f4c00000000000045ad";
    private final TradeDecoder decoder = new TradeDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('P');
        assertThat(decoder.stockLocate()).isEqualTo(4797);
        assertThat(decoder.tracking()).isEqualTo(2);
        assertThat(decoder.timestamp()).isEqualTo(14404329631581L);
        assertThat(decoder.orderRef()).isEqualTo(0L);
        assertThat(decoder.side()).isEqualTo('B');
        assertThat(decoder.shares()).isEqualTo(50L);
        assertThat(decoder.stock()).isEqualTo(5497010720067297312L);
        assertThat(decoder.price()).isEqualTo(2068300L);
        assertThat(decoder.matchNumber()).isEqualTo(17837L);
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            TradeDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.orderRef()).isEqualTo(-1L);
            assertThat(d.side()).isEqualTo((char) 0xFF);
            assertThat(d.shares()).isEqualTo(4_294_967_295L);
            assertThat(d.stock()).isEqualTo(-1L);
            assertThat(d.price()).isEqualTo(4_294_967_295L);
            assertThat(d.matchNumber()).isEqualTo(-1L);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(44).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'P');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.put((byte) 0xFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        return bb.array();
    }
}
