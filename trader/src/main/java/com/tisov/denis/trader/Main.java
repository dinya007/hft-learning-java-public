package com.tisov.denis.trader;

import com.tisov.denis.trader.reader.ItchFileReader;
import com.tisov.denis.trader.walker.ItchMdWalker;
import com.tisov.denis.wire.itch.Itch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    private static final String SAMPLE = "data/01302020.NASDAQ_ITCH50.5mb.itch";

    static void main() throws IOException {
        Path file = Path.of(SAMPLE);

        ItchFileReader reader = new ItchFileReader();
        long[] orders = {0};
        long messages = reader.read(file, new ItchMdWalker(
                o -> {
                    if (orders[0] < 5) {
                        System.out.printf("order ref=%d side=%c px=%.4f qty=%d stock=%s%n",
                                o.orderRef(), o.side(), o.price() / (double) Itch.PRICE_SCALE,
                                o.shares(), unpack(o.stock()));
                    }
                    orders[0]++;
                }
        ));

        System.out.printf("messages=%d  orders(A+F)=%d%n", messages, orders[0]);
    }

    private static String unpack(long packed) {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) (packed & 0xFF);
            packed >>>= 8;
        }
        return new String(b, StandardCharsets.US_ASCII).trim();
    }

    private static Path locateSample() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParent()) {
            Path c = dir.resolve("data").resolve(SAMPLE);
            if (Files.exists(c)) return c;
        }
        return null;
    }
}
