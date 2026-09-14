package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class SystemEventDecoderTest extends Assertions {

    private static final String REAL = "530000000009f649c80cd34f";
    private final SystemEventDecoder decoder = new SystemEventDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('S');
        assertThat(decoder.stockLocate()).isEqualTo(0);
        assertThat(decoder.tracking()).isEqualTo(0);
        assertThat(decoder.timestamp()).isEqualTo(10953404452051L);
        assertThat(decoder.eventCode()).isEqualTo('O');
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            SystemEventDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.eventCode()).isEqualTo((char) 0xFF);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'S');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.put((byte) 0xFF);
        return bb.array();
    }
}
