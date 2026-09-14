package com.tisov.denis.e2e.utils;

import com.tisov.denis.e2e.Replay;
import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.sink.MdEventSink;
import com.tisov.denis.trader.walker.ItchMdWalker;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Requires data/01302020.NASDAQ_ITCH50.itch (full ~12 GB NASDAQ ITCH 5.0 feed, not in the repo)")
public class MarketStatsReport {

    private static final Path SAMPLE_FILE = Path.of("..").toAbsolutePath()
            .resolve("data/01302020.NASDAQ_ITCH50.5mb.itch");
    private static final Path FULL_FILE = Path.of("..").toAbsolutePath()
            .resolve("data/01302020.NASDAQ_ITCH50.itch");

    private static final Path FILE = FULL_FILE;
    private static final int TOP_N = 20;
    private static final long TICK = 100;
    private static final long PRICE_SCALE = 10_000;
    private static final int LOCATES = 1 << 16;
    private static final double BAND_LO_PCT = 0.1;
    private static final double BAND_HI_PCT = 99.9;

    @Test
    void topTradedTickersWithBounds() throws IOException {
        MemorySegment seg = Replay.map(FILE);

        Stats s = new Stats();
        new ItchMdWalker(s).walk(seg);

        int[] ranked = IntStream.range(0, LOCATES)
                .filter(loc -> s.orders[loc] > 0)
                .boxed()
                .sorted(Comparator.comparingLong((Integer loc) -> s.execVol[loc]).reversed())
                .mapToInt(Integer::intValue)
                .toArray();
        int n = Math.min(TOP_N, ranked.length);

        int[] locToTop = new int[LOCATES];
        Arrays.fill(locToTop, -1);
        Histogram[] px = new Histogram[n];
        for (int i = 0; i < n; i++) {
            locToTop[ranked[i]] = i;
            px[i] = new Histogram(3);
        }
        new ItchMdWalker(e -> {
            if (e.type() == MdEvent.TYPE_ADD || e.type() == MdEvent.TYPE_REPLACE) {
                int ti = locToTop[e.locate()];
                if (ti >= 0 && e.price() > 0) px[ti].recordValue(e.price());
            }
        }).walk(seg);

        long[] bandLo = new long[n];
        long[] bandHi = new long[n];
        for (int i = 0; i < n; i++) {
            bandLo[i] = snap(px[i].getValueAtPercentile(BAND_LO_PCT));
            bandHi[i] = snap(px[i].getValueAtPercentile(BAND_HI_PCT));
        }

        FarLevels far = new FarLevels(locToTop, bandLo, bandHi, n);
        new ItchMdWalker(far).walk(seg);

        String bandLabel = "p" + BAND_LO_PCT + "..p" + BAND_HI_PCT + " $";
        System.out.printf("symbols=%d  file=%s%n", ranked.length, FILE.getFileName());
        System.out.printf("%-4s %-8s %7s %14s %10s %22s %22s %8s %9s %9s %9s %9s %9s%n",
                "#", "ticker", "locate", "execVol", "orders", "raw[min..max]$", bandLabel,
                "levels", "peakLive", "poolCap", "peakFar", "ovflCap", "maxQty");
        for (int i = 0; i < n; i++) {
            int loc = ranked[i];
            long levels = (bandHi[i] - bandLo[i]) / TICK + 1;
            System.out.printf("%-4d %-8s %7d %14d %10d %10.4f..%-10.4f %10.4f..%-10.4f %8d %9d %9d %9d %9d %9d%n",
                    i + 1, unpack(s.stock[loc]), loc, s.execVol[loc], s.orders[loc],
                    s.minPx[loc] / (double) PRICE_SCALE, s.maxPx[loc] / (double) PRICE_SCALE,
                    bandLo[i] / (double) PRICE_SCALE, bandHi[i] / (double) PRICE_SCALE,
                    levels, s.peakLive[loc], nextPow2(s.peakLive[loc]),
                    far.peakFar[i], nextPow2(far.peakFar[i]), s.maxShares[loc]);
        }

        long total = 0;
        for (long c : s.typeCount) total += c;
        System.out.printf("%nbook-touching events by type:%n");
        printType(s, MdEvent.TYPE_ADD, "A/F add", total);
        printType(s, MdEvent.TYPE_EXECUTE, "E execute", total);
        printType(s, MdEvent.TYPE_EXECUTE_PRICE, "C exec@px", total);
        printType(s, MdEvent.TYPE_CANCEL, "X cancel", total);
        printType(s, MdEvent.TYPE_DELETE, "D delete", total);
        printType(s, MdEvent.TYPE_REPLACE, "U replace", total);
        System.out.printf("  %-10s %,14d%n", "total", total);

        assertThat(n).isPositive();
    }

    private static void printType(Stats s, byte type, String label, long total) {
        long c = s.typeCount[type];
        System.out.printf("  %-10s %,14d  %5.1f%%%n", label, c, total == 0 ? 0.0 : 100.0 * c / total);
    }

    private static final class Stats implements MdEventSink {
        final long[] stock = new long[LOCATES];
        final long[] orders = new long[LOCATES];
        final long[] execVol = new long[LOCATES];
        final long[] minPx = new long[LOCATES];
        final long[] maxPx = new long[LOCATES];
        final long[] maxShares = new long[LOCATES];
        final long[] typeCount = new long[7];

        final int[] live = new int[LOCATES];
        final int[] peakLive = new int[LOCATES];
        final Long2LongOpenHashMap remaining = new Long2LongOpenHashMap();

        Stats() {
            Arrays.fill(minPx, Long.MAX_VALUE);
            Arrays.fill(maxPx, Long.MIN_VALUE);
            remaining.defaultReturnValue(Long.MIN_VALUE);
        }

        @Override
        public void onEvent(MdEvent e) {
            int loc = e.locate();
            typeCount[e.type()]++;
            switch (e.type()) {
                case MdEvent.TYPE_ADD -> {
                    if (stock[loc] == 0) stock[loc] = e.stock();
                    orders[loc]++;
                    bound(loc, e.price());
                    if (e.shares() > maxShares[loc]) maxShares[loc] = e.shares();
                    remaining.put(e.orderRef(), e.shares());
                    if (++live[loc] > peakLive[loc]) peakLive[loc] = live[loc];
                }
                case MdEvent.TYPE_REPLACE -> {
                    bound(loc, e.price());
                    if (e.shares() > maxShares[loc]) maxShares[loc] = e.shares();
                    long prev = remaining.remove(e.orderRef());
                    remaining.put(e.newOrderRef(), e.shares());
                    if (prev == Long.MIN_VALUE && ++live[loc] > peakLive[loc]) peakLive[loc] = live[loc];
                }
                case MdEvent.TYPE_EXECUTE, MdEvent.TYPE_EXECUTE_PRICE -> {
                    execVol[loc] += e.shares();
                    reduce(loc, e.orderRef(), e.shares());
                }
                case MdEvent.TYPE_CANCEL -> reduce(loc, e.orderRef(), e.shares());
                case MdEvent.TYPE_DELETE -> {
                    if (remaining.remove(e.orderRef()) != Long.MIN_VALUE) live[loc]--;
                }
                default -> {
                }
            }
        }

        private void reduce(int loc, long ref, long qty) {
            long rem = remaining.get(ref);
            if (rem == Long.MIN_VALUE) return;
            rem -= qty;
            if (rem <= 0) {
                remaining.remove(ref);
                live[loc]--;
            } else {
                remaining.put(ref, rem);
            }
        }

        private void bound(int loc, long price) {
            if (price < minPx[loc]) minPx[loc] = price;
            if (price > maxPx[loc]) maxPx[loc] = price;
        }
    }

    private static final class FarLevels implements MdEventSink {
        private final int[] locToTop;
        private final long[] lo;
        private final long[] hi;
        final int[] peakFar;
        private final int[] curFar;
        private final Long2LongOpenHashMap[] levelShares;
        private final Long2LongOpenHashMap refPrice = new Long2LongOpenHashMap();
        private final Long2LongOpenHashMap refShares = new Long2LongOpenHashMap();

        FarLevels(int[] locToTop, long[] lo, long[] hi, int n) {
            this.locToTop = locToTop;
            this.lo = lo;
            this.hi = hi;
            this.peakFar = new int[n];
            this.curFar = new int[n];
            this.levelShares = new Long2LongOpenHashMap[n];
            for (int i = 0; i < n; i++) {
                levelShares[i] = new Long2LongOpenHashMap();
                levelShares[i].defaultReturnValue(0L);
            }
            refPrice.defaultReturnValue(Long.MIN_VALUE);
            refShares.defaultReturnValue(0L);
        }

        @Override
        public void onEvent(MdEvent e) {
            int ti = locToTop[e.locate()];
            if (ti < 0) return;
            switch (e.type()) {
                case MdEvent.TYPE_ADD -> {
                    if (e.price() > 0 && isFar(ti, e.price())) addFar(ti, e.orderRef(), e.price(), e.shares());
                }
                case MdEvent.TYPE_REPLACE -> {
                    removeRef(ti, e.orderRef());
                    if (e.price() > 0 && isFar(ti, e.price())) addFar(ti, e.newOrderRef(), e.price(), e.shares());
                }
                case MdEvent.TYPE_EXECUTE, MdEvent.TYPE_EXECUTE_PRICE, MdEvent.TYPE_CANCEL ->
                        reduceRef(ti, e.orderRef(), e.shares());
                case MdEvent.TYPE_DELETE -> removeRef(ti, e.orderRef());
                default -> { }
            }
        }

        private boolean isFar(int ti, long price) {
            return price < lo[ti] || price > hi[ti];
        }

        private void addFar(int ti, long ref, long price, long shares) {
            refPrice.put(ref, price);
            refShares.put(ref, shares);
            long before = levelShares[ti].get(price);
            levelShares[ti].put(price, before + shares);
            if (before <= 0 && shares > 0 && ++curFar[ti] > peakFar[ti]) peakFar[ti] = curFar[ti];
        }

        private void reduceRef(int ti, long ref, long qty) {
            long price = refPrice.get(ref);
            if (price == Long.MIN_VALUE) return;
            long rem = refShares.get(ref) - qty;
            if (rem <= 0) {
                refPrice.remove(ref);
                refShares.remove(ref);
            } else {
                refShares.put(ref, rem);
            }
            drainLevel(ti, price, qty);
        }

        private void removeRef(int ti, long ref) {
            long price = refPrice.remove(ref);
            if (price == Long.MIN_VALUE) return;
            drainLevel(ti, price, refShares.remove(ref));
        }

        private void drainLevel(int ti, long price, long delta) {
            long after = levelShares[ti].get(price) - delta;
            if (after <= 0) {
                levelShares[ti].remove(price);
                curFar[ti]--;
            } else {
                levelShares[ti].put(price, after);
            }
        }
    }

    private static long snap(long px) {
        return Math.round(px / (double) TICK) * TICK;
    }

    private static int nextPow2(int v) {
        return v <= 1 ? 1 : Integer.highestOneBit(v - 1) << 1;
    }

    private static String unpack(long packed) {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) (packed & 0xFF);
            packed >>>= 8;
        }
        return new String(b, StandardCharsets.US_ASCII).trim();
    }
}
