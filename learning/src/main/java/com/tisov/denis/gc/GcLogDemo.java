package com.tisov.denis.gc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

public class GcLogDemo {

    private static final int SHORT_BYTES = 1024;
    private static final int LONG_BYTES = 4096;
    private static final int LIVE_SET_COUNT = 30_000;
    private static final int BATCH_SIZE = 1_000;

    private static int sink;

    public static void main(String[] args) {
        int durationSec = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        long endNanos = System.nanoTime() + TimeUnit.SECONDS.toMillis(durationSec);

        Deque<byte[]> liveSet = new ArrayDeque<>(LIVE_SET_COUNT);
        for (int i = 0; i < LIVE_SET_COUNT; i++) {
            liveSet.addLast(new byte[LONG_BYTES]);
        }
        long liveSetMb = (long) LIVE_SET_COUNT * LONG_BYTES / (1024 * 1024);
        System.out.printf("Live set: %,d × %d B = %d MB%n", LIVE_SET_COUNT, LONG_BYTES, liveSetMb);
        System.out.printf("Duration: %d s%n%n", durationSec);

        long allocCount = 0;
        long startNanos = System.nanoTime();
        long nextProgressNanos = startNanos + TimeUnit.SECONDS.toMillis(5);

        while (System.nanoTime() < endNanos) {
            for (int i = 0; i < BATCH_SIZE; i++) {
                byte[] junk = new byte[SHORT_BYTES];
                sink ^= junk.length;
            }
            allocCount += BATCH_SIZE;

            liveSet.pollFirst();
            liveSet.addLast(new byte[LONG_BYTES]);

            if (System.nanoTime() > nextProgressNanos) {
                double elapsed = (System.nanoTime() - startNanos) / 1e9;
                double allocMbPerSec = (allocCount * (double) SHORT_BYTES) / (elapsed * 1024 * 1024);
                System.out.printf("t=%5.1fs  allocs=%,12d  short-lived=%.0f MB/s%n",
                        elapsed, allocCount, allocMbPerSec);
                nextProgressNanos += 5_000_000_000L;
            }
        }

        double elapsed = (System.nanoTime() - startNanos) / 1e9;
        double avgMbPerSec = (allocCount * (double) SHORT_BYTES) / (elapsed * 1024 * 1024);
        System.out.printf("%nDone. allocs=%,d retained=%d avg=%.0f MB/s sink=%d%n",
                allocCount, liveSet.size(), avgMbPerSec, sink);
    }
}
