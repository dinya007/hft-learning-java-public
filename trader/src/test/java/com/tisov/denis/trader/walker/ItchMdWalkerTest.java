package com.tisov.denis.trader.walker;

import com.sun.management.ThreadMXBean;
import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.sink.MdEventSink;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

class ItchMdWalkerTest {

    private static final int WARM_UP_CYCLES = 100;
    private static final int TEST_CYCLES = 100;

    ItchMdWalker walker = new ItchMdWalker(new CountingSink());

    @Test
    void allocatesNoMemory() throws IOException {
        ThreadMXBean tb = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertThat(tb.isThreadAllocatedMemorySupported()).isTrue();
        tb.setThreadAllocatedMemoryEnabled(true);

        try (Arena arena = Arena.ofConfined();
             FileChannel fileChannel = FileChannel.open(TestUtils.SAMPLE_FILE, StandardOpenOption.READ)) {
            MemorySegment memorySegment = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size(), arena);
            long warm = 0;
            for (int i = 0; i < WARM_UP_CYCLES; i++) {
                warm += walker.walk(memorySegment);
            }

            assertThat(warm).isPositive();

            long tid = Thread.currentThread().threadId();
            long before = tb.getThreadAllocatedBytes(tid);
            long messages = 0;
            for (int i = 0; i < TEST_CYCLES; i++) {
                messages += walker.walk(memorySegment);
            }
            long after = tb.getThreadAllocatedBytes(tid);

            assertThat(messages).isEqualTo(441470 * TEST_CYCLES);
            assertThat(after - before)
                    .as("hot walk must allocate 0 bytes (messages=%d)", messages)
                    .isZero();

        }

    }

    private static final class CountingSink implements MdEventSink {
        long count;

        @Override
        public void onEvent(MdEvent mdEvent) {
            count++;

        }
    }
}
