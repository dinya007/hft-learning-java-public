package com.tisov.denis.trader.book.side;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Threads(1)
public class DivisionBy100Benchmark {

    private static final int CHAIN = 1_000;
    private static final long M100 = 0xA3D70A3D70A3D70BL;

    private long tickSize = 100;
    private final long tickSizeFinal = 100;
    private long add = 1_000_000;
    private long seed = 1_500_000;
    private long tickReciprocal;

    @Setup
    public void setup() {
        tickReciprocal = (0x1_0000_0000L + tickSize - 1) / tickSize;
    }

    @Benchmark
    @OperationsPerInvocation(CHAIN)
    public long baseline() {
        long x = seed;
        for (int j = 0; j < CHAIN; j++) {
            x = (x >>> 7) + add;
        }
        return x;
    }

    @Benchmark
    @OperationsPerInvocation(CHAIN)
    public long division() {
        long x = seed;
        for (int j = 0; j < CHAIN; j++) {
            x = (x + add) / tickSize;
        }
        return x;
    }

    @Benchmark
    @OperationsPerInvocation(CHAIN)
    public long divisionFinal() {
        long x = seed;
        for (int j = 0; j < CHAIN; j++) {
            x = (x + add) / tickSizeFinal;
        }
        return x;
    }

    @Benchmark
    @OperationsPerInvocation(CHAIN)
    public long reciprocal() {
        long x = seed;
        for (int j = 0; j < CHAIN; j++) {
            x = ((x + add) * tickReciprocal) >>> 32;
        }
        return x;
    }

    @Benchmark
    @OperationsPerInvocation(CHAIN)
    public long multiplyHigh() {
        long x = seed;
        for (int j = 0; j < CHAIN; j++) {
            x = Math.unsignedMultiplyHigh(x + add, M100) >>> 6;
        }
        return x;
    }
}
