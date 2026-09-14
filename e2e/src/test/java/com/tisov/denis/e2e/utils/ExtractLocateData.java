package com.tisov.denis.e2e;

import com.tisov.denis.wire.itch.Itch;
import com.tisov.denis.wire.itch.decoder.AddOrderDecoder;
import com.tisov.denis.wire.itch.decoder.AddOrderWithMpidDecoder;
import com.tisov.denis.wire.itch.decoder.OrderCancelDecoder;
import com.tisov.denis.wire.itch.decoder.OrderDeleteDecoder;
import com.tisov.denis.wire.itch.decoder.OrderExecutedDecoder;
import com.tisov.denis.wire.itch.decoder.OrderExecutedWithPriceDecoder;
import com.tisov.denis.wire.itch.decoder.OrderReplaceDecoder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Requires data/01302020.NASDAQ_ITCH50.itch (full ~12 GB NASDAQ ITCH 5.0 feed, not in the repo)")
public class ExtractLocateData {

    private static final Path DATA = Path.of("..").toAbsolutePath().resolve("data");
    private static final Path FULL_FILE = DATA.resolve("01302020.NASDAQ_ITCH50.itch");

    private static final TickerConfig TICKER = Tickers.AMD;
    private static final int LOCATE = TICKER.locate();
    private static final Path OUT_DIR = DATA.resolve(TICKER.ticker());
    private static final String PREFIX = "01302020";

    private static final long NS = 1_000_000_000L;
    private static final int SECONDS = 26 * 3600;

    private static final String ALL = PREFIX + ".all.itch";
    private static final int[] WIN = {5, 60, 300};
    private static final String[] OUT = {
            PREFIX + ".peak_5s.itch",
            PREFIX + ".peak_1m.itch",
            PREFIX + ".peak_5m.itch",
    };

    @Test
    void extract() throws IOException {
        MemorySegment seg = Replay.map(FULL_FILE);
        long size = seg.byteSize();

        long[] perSec = new long[SECONDS];
        long count = 0, minTs = Long.MAX_VALUE, maxTs = Long.MIN_VALUE;
        for (long off = 0; off + 2 < size; ) {
            long msgOffset = off + 2;
            char type = Itch.readChar(seg, msgOffset);
            int len = Itch.messageLength(type);
            if (isBookTouching(type) && Itch.readU16BE(seg, msgOffset + 1) == LOCATE) {
                long ts = Itch.readU48BE(seg, msgOffset + 5);
                int s = (int) (ts / NS);
                if (s >= 0 && s < SECONDS) perSec[s]++;
                count++;
                if (ts < minTs) minTs = ts;
                if (ts > maxTs) maxTs = ts;
            }
            off += 2 + len;
        }

        System.out.printf("%n%s (locate=%d)  events=%,d  first=%s  last=%s%n", TICKER.ticker(), LOCATE, count, clock(minTs), clock(maxTs));

        long[] startNs = new long[3];
        long[] endNs = new long[3];
        for (int w = 0; w < WIN.length; w++) {
            int startSec = busiestStart(perSec, WIN[w]);
            startNs[w] = startSec * NS;
            endNs[w] = (startSec + WIN[w]) * NS;
            System.out.printf("peak %-3s busiest: %s .. %s  events=%,d  avg=%,d ev/s  ns [%d, %d)%n",
                    WIN[w] + "s", clock(startNs[w]), clock(endNs[w]),
                    windowSum(perSec, startSec, WIN[w]), windowSum(perSec, startSec, WIN[w]) / WIN[w],
                    startNs[w], endNs[w]);
        }
        System.out.println();

        writeOutputs(seg, size, startNs, endNs);

        assertThat(count).isPositive();
    }

    private void writeOutputs(MemorySegment seg, long size, long[] startNs, long[] endNs) throws IOException {
        Files.createDirectories(OUT_DIR);

        AddOrderDecoder add = new AddOrderDecoder();
        AddOrderWithMpidDecoder addMpid = new AddOrderWithMpidDecoder();
        OrderExecutedDecoder exec = new OrderExecutedDecoder();
        OrderExecutedWithPriceDecoder execPx = new OrderExecutedWithPriceDecoder();
        OrderCancelDecoder cancel = new OrderCancelDecoder();
        OrderDeleteDecoder delete = new OrderDeleteDecoder();
        OrderReplaceDecoder replace = new OrderReplaceDecoder();

        Long2ObjectOpenHashMap<Live> live = new Long2ObjectOpenHashMap<>(1 << 15);
        OutputStream all = new BufferedOutputStream(Files.newOutputStream(OUT_DIR.resolve(ALL)), 1 << 20);
        OutputStream[] out = new OutputStream[3];
        boolean[] started = new boolean[3];
        boolean[] done = new boolean[3];
        long[] snapCount = new long[3];
        long[] evCount = new long[3];
        long allCount = 0;
        byte[] frame = new byte[64];
        byte[] snap = new byte[38];

        for (long off = 0; off + 2 < size; ) {
            long msgOffset = off + 2;
            char type = Itch.readChar(seg, msgOffset);
            int len = Itch.messageLength(type);
            if (isBookTouching(type) && Itch.readU16BE(seg, msgOffset + 1) == LOCATE) {
                long ts = Itch.readU48BE(seg, msgOffset + 5);
                int total = 2 + len;
                MemorySegment.copy(seg, JAVA_BYTE, off, frame, 0, total);

                all.write(frame, 0, total);
                allCount++;

                for (int w = 0; w < 3; w++) {
                    if (!started[w] && ts >= startNs[w]) {
                        out[w] = new BufferedOutputStream(Files.newOutputStream(OUT_DIR.resolve(OUT[w])), 1 << 20);
                        snapCount[w] = emitSnapshot(out[w], live, startNs[w] - 1, snap);
                        started[w] = true;
                    }
                    if (started[w] && !done[w] && ts >= endNs[w]) {
                        out[w].close();
                        done[w] = true;
                    }
                    if (started[w] && !done[w]) {
                        out[w].write(frame, 0, total);
                        evCount[w]++;
                    }
                }
                updateLive(seg, type, msgOffset, live, add, addMpid, exec, execPx, cancel, delete, replace);
            }
            off += 2 + len;
        }

        all.close();
        for (int w = 0; w < 3; w++) {
            if (started[w] && !done[w]) out[w].close();
        }

        System.out.printf("%-26s  events=%,d  bytes=%,d%n", ALL, allCount, Files.size(OUT_DIR.resolve(ALL)));
        for (int w = 0; w < 3; w++) {
            System.out.printf("%-26s  snapshot=%,d  window=%,d  bytes=%,d%n",
                    OUT[w], snapCount[w], evCount[w], Files.size(OUT_DIR.resolve(OUT[w])));
        }
    }

    private static long emitSnapshot(OutputStream out, Long2ObjectOpenHashMap<Live> live, long ts, byte[] f) throws IOException {
        putU16BE(f, 0, Itch.LEN_ADD_ORDER);
        f[2] = (byte) Itch.ADD_ORDER;
        putU16BE(f, 3, LOCATE);
        putU16BE(f, 5, 0);
        putU48BE(f, 7, ts);
        long n = 0;
        for (Long2ObjectOpenHashMap.Entry<Live> e : live.long2ObjectEntrySet()) {
            Live o = e.getValue();
            putU64BE(f, 13, e.getLongKey());
            f[21] = (byte) o.side;
            putU32BE(f, 22, o.shares);
            putU64BE(f, 26, o.stock);
            putU32BE(f, 34, o.price);
            out.write(f, 0, 38);
            n++;
        }
        return n;
    }

    private static void updateLive(MemorySegment seg, char type, long msgOffset, Long2ObjectOpenHashMap<Live> live,
                                   AddOrderDecoder add, AddOrderWithMpidDecoder addMpid,
                                   OrderExecutedDecoder exec, OrderExecutedWithPriceDecoder execPx,
                                   OrderCancelDecoder cancel, OrderDeleteDecoder delete, OrderReplaceDecoder replace) {
        switch (type) {
            case Itch.ADD_ORDER -> {
                add.wrap(seg, msgOffset);
                live.put(add.orderRef(), new Live(add.price(), add.shares(), add.side(), add.stock()));
            }
            case Itch.ADD_ORDER_MPID -> {
                addMpid.wrap(seg, msgOffset);
                live.put(addMpid.orderRef(), new Live(addMpid.price(), addMpid.shares(), addMpid.side(), addMpid.stock()));
            }
            case Itch.ORDER_EXECUTED -> {
                exec.wrap(seg, msgOffset);
                reduce(live, exec.orderRef(), exec.executedShares());
            }
            case Itch.ORDER_EXECUTED_PRICE -> {
                execPx.wrap(seg, msgOffset);
                reduce(live, execPx.orderRef(), execPx.executedShares());
            }
            case Itch.ORDER_CANCEL -> {
                cancel.wrap(seg, msgOffset);
                reduce(live, cancel.orderRef(), cancel.cancelledShares());
            }
            case Itch.ORDER_DELETE -> {
                delete.wrap(seg, msgOffset);
                live.remove(delete.orderRef());
            }
            case Itch.ORDER_REPLACE -> {
                replace.wrap(seg, msgOffset);
                Live o = live.remove(replace.originalOrderRef());
                if (o != null) {
                    live.put(replace.newOrderRef(), new Live(replace.price(), replace.shares(), o.side, o.stock));
                }
            }
            default -> { }
        }
    }

    private static void reduce(Long2ObjectOpenHashMap<Live> live, long ref, long qty) {
        Live o = live.get(ref);
        if (o == null) return;
        o.shares -= qty;
        if (o.shares <= 0) live.remove(ref);
    }

    private static int busiestStart(long[] perSec, int win) {
        long sum = 0;
        for (int i = 0; i < win; i++) sum += perSec[i];
        long best = sum;
        int bestStart = 0;
        for (int i = win; i < perSec.length; i++) {
            sum += perSec[i] - perSec[i - win];
            if (sum > best) { best = sum; bestStart = i - win + 1; }
        }
        return bestStart;
    }

    private static long windowSum(long[] perSec, int startSec, int win) {
        long sum = 0;
        for (int i = startSec; i < startSec + win && i < perSec.length; i++) sum += perSec[i];
        return sum;
    }

    private static boolean isBookTouching(char t) {
        return t == Itch.ADD_ORDER || t == Itch.ADD_ORDER_MPID
                || t == Itch.ORDER_EXECUTED || t == Itch.ORDER_EXECUTED_PRICE
                || t == Itch.ORDER_CANCEL || t == Itch.ORDER_DELETE || t == Itch.ORDER_REPLACE;
    }

    private static void putU16BE(byte[] b, int o, int v) {
        b[o] = (byte) (v >>> 8);
        b[o + 1] = (byte) v;
    }

    private static void putU32BE(byte[] b, int o, long v) {
        b[o] = (byte) (v >>> 24);
        b[o + 1] = (byte) (v >>> 16);
        b[o + 2] = (byte) (v >>> 8);
        b[o + 3] = (byte) v;
    }

    private static void putU48BE(byte[] b, int o, long v) {
        putU16BE(b, o, (int) (v >>> 32));
        putU32BE(b, o + 2, v);
    }

    private static void putU64BE(byte[] b, int o, long v) {
        putU32BE(b, o, v >>> 32);
        putU32BE(b, o + 4, v);
    }

    private static String clock(long ts) {
        long totalMs = ts / 1_000_000L;
        long ms = totalMs % 1000;
        long totalS = totalMs / 1000;
        long s = totalS % 60, m = (totalS / 60) % 60, h = totalS / 3600;
        return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
    }

    private static final class Live {
        long price;
        long shares;
        long stock;
        char side;

        Live(long price, long shares, char side, long stock) {
            this.price = price;
            this.shares = shares;
            this.side = side;
            this.stock = stock;
        }
    }
}
