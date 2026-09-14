package com.tisov.denis.memoryorder;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 2, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 400, timeUnit = TimeUnit.MILLISECONDS)
public class CounterBenchmark {

    Counter counter = new Counter();

    @Benchmark
    public long get_no_contention() {
        return counter.get();
    }

    @Benchmark
    @Threads(4)
    public long get_contention() {
        return counter.get();
    }

    @Benchmark
    public long incrementPlain_no_contention() {
        return counter.incrementPlain();
    }

    @Benchmark
    @Threads(4)
    public long incrementPlain_contention() {
        return counter.incrementPlain();
    }

    @Benchmark
    public long incrementPlainVar_no_contention() {
        return counter.incrementPlainVar();
    }

    @Benchmark
    @Threads(4)
    public long incrementPlainVar_contention() {
        return counter.incrementPlainVar();
    }

    @Benchmark
    public long incrementRelaxed_no_contention() {
        return counter.incrementRelaxed();
    }

    @Benchmark
    @Threads(4)
    public long incrementRelaxed_contention() {
        return counter.incrementRelaxed();
    }

    @Benchmark
    public long incrementAcquireRelease_no_contention() {
        return counter.incrementAcquireRelease();
    }

    @Benchmark
    @Threads(4)
    public long incrementAcquireRelease_contention() {
        return counter.incrementAcquireRelease();
    }

    @Benchmark
    public long incrementVolatile_no_contention() {
        return counter.incrementVolatile();
    }

    @Benchmark
    @Threads(4)
    public long incrementVolatile_contention() {
        return counter.incrementVolatile();
    }

    @Benchmark
    public long incrementCAS_no_contention() {
        return counter.incrementCAS();
    }

    @Benchmark
    @Threads(4)
    public long incrementCAS_contention() {
        return counter.incrementCAS();
    }

    @Benchmark
    public long incrementGetAndAdd_no_contention() {
        return counter.incrementGetAndAdd();
    }

    @Benchmark
    @Threads(4)
    public long incrementGetAndAdd_contention() {
        return counter.incrementGetAndAdd();
    }
}
