package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.domain.Side;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 1, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Threads(1)
public class BookSideBenchmark {

    private static final int BATCH = 1_000;

    private static final long TICK_SIZE = 1_00;
    private static final long MIN_PRICE = 1_500_000;

    @Param({"1024"})
    public int bookDepth;

    private static final int OUT_OF_INITIAL_RANGE_DEPTH = 1024;

    @Param({"treemap", "fastutil", "ladder"})
    public String impl;

    private BookSide side;
    private long[] tickPrices;
    private int i;

    @Setup(Level.Trial)
    public void setupTrial() {
        tickPrices = new long[bookDepth];
        for (int i = 0; i < tickPrices.length; i++) {
            tickPrices[i] = MIN_PRICE + (long) i * TICK_SIZE;
        }
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        side = switch (impl) {
            case "treemap" -> new TreeMapBookSide(Side.BID);
            case "fastutil" -> new Long2LongRBTreeMapBookSide(Side.BID);
            case "ladder" -> new FixedWindowLadderBookSide(Side.BID, MIN_PRICE, 1_00, bookDepth + OUT_OF_INITIAL_RANGE_DEPTH);
            default -> throw new IllegalArgumentException(impl);
        };

        prefill();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void updateHotLevels(Blackhole blackhole) {
        for (int j = 0; j < BATCH; j++) {
            int top = bookDepth - 1;
            long p = tickPrices[top - (i++ & 3)];
            side.add(p, 100);
            side.remove(p, 100);
            blackhole.consume(side.bestPrice());
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void insertDeleteLevels(Blackhole blackhole) {
        for (int j = 0; j < BATCH; j++) {
            long p = MIN_PRICE + (long) (bookDepth + (i++ & 1023)) * TICK_SIZE;
            side.add(p, 100);
            side.remove(p, 100);
            blackhole.consume(side.bestPrice());
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void sweepBest(Blackhole blackhole) {
        for (int j = 0; j < BATCH; j++) {
            long best = side.bestPrice();
            side.remove(best, 100);
            long newBest = side.bestPrice();
            side.add(best, 100);
            blackhole.consume(newBest);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void readBest(Blackhole blackhole) {
        for (int j = 0; j < BATCH; j++) {
            blackhole.consume(side.bestPrice() ^ side.bestShares());
        }
    }

    private void prefill() {
        for (long tickPrice : tickPrices) {
            side.add(tickPrice, 100);
        }
    }

}
