package com.tisov.denis.wire.itch.decoder;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

class StockDirectoryDecoderTest extends Assertions {

    private static final String REAL = "52000100000a37d4c8050b41202020202020204e20000000644e435a20504e20314e000000004e";
    private final StockDirectoryDecoder decoder = new StockDirectoryDecoder();

    @Test
    void decodesReal() {
        byte[] bytes = HexFormat.of().parseHex(REAL);
        MemorySegment memorySegment = MemorySegment.ofArray(bytes);

        decoder.wrap(memorySegment, 0);

        assertThat(decoder.messageType()).isEqualTo('R');
        assertThat(decoder.stockLocate()).isEqualTo(1);
        assertThat(decoder.tracking()).isEqualTo(0);
        assertThat(decoder.timestamp()).isEqualTo(11234909357323L);
        assertThat(decoder.stock()).isEqualTo(4692786134070075424L);
        assertThat(decoder.marketCategory()).isEqualTo('N');
        assertThat(decoder.financialStatus()).isEqualTo(' ');
        assertThat(decoder.roundLotSize()).isEqualTo(100L);
        assertThat(decoder.roundLotsOnly()).isEqualTo('N');
        assertThat(decoder.issueClassification()).isEqualTo('C');
        assertThat(decoder.issueSubType()).isEqualTo(23072);
        assertThat(decoder.authenticity()).isEqualTo('P');
        assertThat(decoder.shortSaleThreshold()).isEqualTo('N');
        assertThat(decoder.ipoFlag()).isEqualTo(' ');
        assertThat(decoder.luldReferencePriceTier()).isEqualTo('1');
        assertThat(decoder.etpFlag()).isEqualTo('N');
        assertThat(decoder.etpLeverageFactor()).isEqualTo(0L);
        assertThat(decoder.inverseIndicator()).isEqualTo('N');
    }

    @Test
    void decodesUnsignedMax() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] msg = buildCornerCase();
            MemorySegment seg = arena.allocate(3 + msg.length + 2);
            MemorySegment.copy(msg, 0, seg, JAVA_BYTE, 3, msg.length);

            StockDirectoryDecoder d = decoder.wrap(seg, 3);

            assertThat(d.stockLocate()).isEqualTo(65_535);
            assertThat(d.tracking()).isEqualTo(65_535);
            assertThat(d.timestamp()).isEqualTo(281_474_976_710_655L);
            assertThat(d.stock()).isEqualTo(-1L);
            assertThat(d.roundLotSize()).isEqualTo(4_294_967_295L);
            assertThat(d.issueSubType()).isEqualTo(65_535);
            assertThat(d.etpLeverageFactor()).isEqualTo(4_294_967_295L);
            assertThat(d.marketCategory()).isEqualTo((char) 0xFF);
            assertThat(d.inverseIndicator()).isEqualTo((char) 0xFF);
        }
    }

    private static byte[] buildCornerCase() {
        ByteBuffer bb = ByteBuffer.allocate(39).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 'R');
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putShort((short) 0xFFFF);
        bb.putInt(0xFFFFFFFF);
        bb.putLong(-1L);
        bb.put((byte) 0xFF);
        bb.put((byte) 0xFF);
        bb.putInt(0xFFFFFFFF);
        bb.put((byte) 0xFF);
        bb.put((byte) 0xFF);
        bb.putShort((short) 0xFFFF);
        bb.put((byte) 0xFF);
        bb.put((byte) 0xFF);
        bb.put((byte) 0xFF);
        bb.put((byte) 0xFF);
        bb.put((byte) 0xFF);
        bb.putInt(0xFFFFFFFF);
        bb.put((byte) 0xFF);
        return bb.array();
    }
}
