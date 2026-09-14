package com.tisov.denis.e2e;

import com.tisov.denis.trader.sink.MdEventSink;
import com.tisov.denis.trader.walker.ItchMdWalker;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Threads(1)
public class EndToEndReplayBenchmark {

    private static final Path SAMPLE_FILE = Path.of("..").toAbsolutePath()
            .resolve("data/01302020.NASDAQ_ITCH50.5mb.itch");
    private static final Path FULL_FILE = Path.of("..").toAbsolutePath()
            .resolve("data/01302020.NASDAQ_ITCH50.itch");
    private static final Path FILE = SAMPLE_FILE;
    private static final TickerConfig TICKER = Tickers.AMD;

    private MemorySegment segment;
    private ItchMdWalker walker;
    private long emitted;

    @Setup(Level.Trial)
    public void mapFile() throws Exception {
        segment = Replay.map(FILE);
    }

    @Setup(Level.Invocation)
    public void freshPipeline() {
        emitted = 0;
        MdEventSink pipeline = Replay.build(TICKER, i -> emitted += i.price()).sink();
        walker = new ItchMdWalker(new Replay.SingleSymbolFilter(TICKER.locate(), pipeline));
    }

    @Benchmark
    public long replay() {
        return walker.walk(segment) ^ emitted;
    }
}
