package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class AddOrderWithMpidDecoderTest extends Assertions {

    private static final String REAL = "4622cc00000d18c4c4c466000000000000235c53000000645a565a5a5420202000e975a04c45484d";
    private final AddOrderWithMpidDecoder decoder = new AddOrderWithMpidDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('F');
        assertThat(decoder.stockLocate()).isEqualTo(8908);
        assertThat(decoder.tracking()).isEqualTo(0);
        assertThat(decoder.timestamp()).isEqualTo(14400031605862L);
        assertThat(decoder.orderRef()).isEqualTo(9052L);
        assertThat(decoder.side()).isEqualTo('S');
        assertThat(decoder.shares()).isEqualTo(100L);
        assertThat(decoder.stock()).isEqualTo(6509489655415578656L);
        assertThat(decoder.price()).isEqualTo(15300000L);
        assertThat(decoder.attribution()).isEqualTo(1279608909L);
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            AddOrderWithMpidDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.orderRef()).isEqualTo(-1L);
            assertThat(d.side()).isEqualTo((char) 0xFF);
            assertThat(d.shares()).isEqualTo(4_294_967_295L);
            assertThat(d.stock()).isEqualTo(-1L);
            assertThat(d.price()).isEqualTo(4_294_967_295L);
            assertThat(d.attribution()).isEqualTo(4_294_967_295L);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(40).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'F');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.put((byte) 0xFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.putInt(0xFFFFFFFF);
        bb.putInt(0xFFFFFFFF);
        return bb.array();
    }
}
