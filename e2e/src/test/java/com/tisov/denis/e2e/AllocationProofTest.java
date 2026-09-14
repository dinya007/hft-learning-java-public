package com.tisov.denis.e2e;

import com.sun.management.ThreadMXBean;
import com.tisov.denis.trader.sink.MdEventSink;
import com.tisov.denis.trader.walker.ItchMdWalker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Requires data/01302020.NASDAQ_ITCH50.itch (full ~12 GB NASDAQ ITCH 5.0 feed, not in the repo)")
public class AllocationProofTest {

    private static final Path SAMPLE_FILE = Path.of("..").toAbsolutePath()
            .resolve("data/01302020.NASDAQ_ITCH50.5mb.itch");
    private static final Path FULL_FILE = Path.of("..").toAbsolutePath()
            .resolve("data/01302020.NASDAQ_ITCH50.itch");
    private static final int WARMUP_ITERATIONS = 1;
    private static final TickerConfig TICKER = Tickers.AMD;

    @Test
    void walkIsAllocFreeRebuildIsNot() throws IOException {
        ThreadMXBean tb = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertThat(tb.isThreadAllocatedMemorySupported()).as("thread allocation counter unsupported").isTrue();
        tb.setThreadAllocatedMemoryEnabled(true);
        long threadId = Thread.currentThread().threadId();

        MemorySegment memorySegment = Replay.map(FULL_FILE);
        long[] cnt = {0};
        warmup(cnt, memorySegment);

        long bytesBeforeBuild = tb.getThreadAllocatedBytes(threadId);
        MdEventSink pipeline = Replay.build(TICKER, _ -> cnt[0]++).sink();
        ItchMdWalker walker = walk(pipeline);
        long bytesAfterBuild = tb.getThreadAllocatedBytes(threadId);

        cnt[0] = 0;
        walker.walk(memorySegment);
        long bytesAfterWalk = tb.getThreadAllocatedBytes(threadId);

        long rebuild = bytesAfterBuild - bytesBeforeBuild;
        long walk = bytesAfterWalk - bytesAfterBuild;

        System.out.printf("rebuild alloc = %,d B%n", rebuild);
        System.out.printf("walk    alloc = %,d B   (events forwarded = %,d)%n", walk, cnt[0]);

        assertThat(rebuild).isGreaterThan(400_000);
        assertThat(walk).isLessThan(1024);
    }

    private static void warmup(long[] cnt, MemorySegment memorySegment) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            cnt[0] = 0;
            MdEventSink p = Replay.build(TICKER, _ -> cnt[0]++).sink();
            walk(p).walk(memorySegment);
        }
    }

    private static ItchMdWalker walk(MdEventSink pipeline) {
        return new ItchMdWalker(new Replay.SingleSymbolFilter(TICKER.locate(), pipeline));
    }
}
