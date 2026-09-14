package com.tisov.denis.trader.book;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.agrona.collections.Long2LongHashMap;
import org.openjdk.jmh.annotations.*;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Threads(1)
public class HashMapBenchmark {

    private static final int MISSING_VALUE = -1;
    private static final int BATCH = 1024;
    private static final int MAX_VALUE = 100_000;
    private static final int CAPACITY = 1 << 18;
    private static final int CAPACITY_ARG = (CAPACITY >> 1) + 1;
    private static final float LOAD_FACTOR = 0.65f;

    @Param({"fastutil", "agrona"})
    public String mapType;

    private HashMap hashMap;
    private long[] liveRefs;
    private int ring;
    private long nextRef = 1;
    private final SplittableRandom random = new SplittableRandom(42);

    @Setup(Level.Trial)
    public void setup() {
        hashMap = switch (mapType) {
            case "fastutil" -> new FastutilHashMap(CAPACITY_ARG);
            case "agrona" -> new AgronaHashMap(CAPACITY_ARG);
            default -> throw new IllegalArgumentException(mapType);
        };
        liveRefs = new long[MAX_VALUE];
        for (int k = 0; k < MAX_VALUE; k++) {
            long r = nextRef++;
            hashMap.put(r, k);
            liveRefs[k] = r;
        }
        ring = 0;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public int churn() {
        int sink = 0;
        for (int j = 0; j < BATCH; j++) {
            hashMap.remove(liveRefs[ring]);
            long r = nextRef++;
            hashMap.put(r, ring);
            liveRefs[ring] = r;
            sink ^= hashMap.get(liveRefs[random.nextInt(MAX_VALUE)]);
            ring = (ring + 1 == MAX_VALUE) ? 0 : ring + 1;
        }
        return sink;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public int churnProportionally() {
        int sink = 0;
        for (int j = 0; j < BATCH; j++) {
            int phase = j % 10;
            if (phase < 7) {
                hashMap.remove(liveRefs[ring]);
                long ref = nextRef++;
                hashMap.put(ref, ring);
                liveRefs[ring] = ref;
                ring = (ring + 1 == MAX_VALUE) ? 0 : ring + 1;
            } else if (phase < 9) {
                sink ^= hashMap.get(liveRefs[random.nextInt(MAX_VALUE)]);
            } else {
                hashMap.remove(liveRefs[ring]);
                long ref = nextRef++;
                hashMap.put(ref, ring);
                liveRefs[ring] = ref;
                ring = (ring + 1 == MAX_VALUE) ? 0 : ring + 1;
            }
        }
        return sink;
    }

    interface HashMap {
        long get(long ref);

        void put(long ref, int idx);

        int remove(long ref);
    }

    static final class FastutilHashMap implements HashMap {
        private final Long2IntOpenHashMap m;

        FastutilHashMap(int cap) {
            m = new Long2IntOpenHashMap(cap, LOAD_FACTOR);
            m.defaultReturnValue(MISSING_VALUE);
        }

        public long get(long ref) {
            return m.get(ref);
        }

        public void put(long ref, int idx) {
            m.put(ref, idx);
        }

        public int remove(long ref) {
            return m.remove(ref);
        }
    }

    static final class AgronaHashMap implements HashMap {
        private final Long2LongHashMap m;

        AgronaHashMap(int cap) {
            m = new Long2LongHashMap(cap, LOAD_FACTOR, MISSING_VALUE);
        }

        public long get(long ref) {
            return m.get(ref);
        }

        public void put(long ref, int idx) {
            m.put(ref, idx);
        }

        public int remove(long ref) {
            return (int) m.remove(ref);
        }
    }
}
