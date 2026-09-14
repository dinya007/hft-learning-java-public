package com.tisov.denis.falsesharing;

import com.tisov.denis.falsesharing.FalseSharing;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Group)
@Fork(value = 1, jvmArgsPrepend = {"-XX:-RestrictContended", "-XX:ContendedPaddingWidth=128"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class FalseSharingBenchmark {

    FalseSharing falseSharing = new FalseSharing();

    @Benchmark
    @Group("increment_not_contended")
    @GroupThreads(1)
    public long incrementNotContendedA() {
        return falseSharing.incrementA();
    }

    @Benchmark
    @GroupThreads(1)
    @Group("increment_not_contended")
    public long incrementNotContendedB() {
        return falseSharing.incrementB();
    }

    @Benchmark
    @Group("increment_contended")
    @GroupThreads(1)
    public long incrementContendedC() {
        return falseSharing.incrementC();
    }

    @Benchmark
    @GroupThreads(1)
    @Group("increment_contended")
    public long incrementContendedD() {
        return falseSharing.incrementD();
    }

}
