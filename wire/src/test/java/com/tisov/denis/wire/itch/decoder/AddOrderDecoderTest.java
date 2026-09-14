package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class AddOrderDecoderTest extends Assertions {

    private static final String REAL = "4120f000000d18c2ee38b2000000000000000842000005dc564f44202020202000030700";
    private final AddOrderDecoder decoder = new AddOrderDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('A');
        assertThat(decoder.stockLocate()).isEqualTo(8432);
        assertThat(decoder.tracking()).isEqualTo(0);
        assertThat(decoder.timestamp()).isEqualTo(14400000768178L);
        assertThat(decoder.orderRef()).isEqualTo(8L);
        assertThat(decoder.side()).isEqualTo('B');
        assertThat(decoder.shares()).isEqualTo(1500L);
        assertThat(decoder.stock()).isEqualTo(6219264515190562848L);
        assertThat(decoder.price()).isEqualTo(198400L);
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            AddOrderDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.orderRef()).isEqualTo(-1L);
            assertThat(Long.toUnsignedString(d.orderRef())).isEqualTo("18446744073709551615");
            assertThat(d.side()).isEqualTo((char) 0xFF);
            assertThat(d.shares()).isEqualTo(4_294_967_295L);
            assertThat(d.stock()).isEqualTo(-1L);
            assertThat(d.price()).isEqualTo(4_294_967_295L);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'A');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.put((byte) 0xFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.putInt(0xFFFFFFFF);
        return bb.array();
    }

}
