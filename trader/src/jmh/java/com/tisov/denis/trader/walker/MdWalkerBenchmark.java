package com.tisov.denis.trader.walker;

import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.sink.MdEventSink;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import static java.lang.foreign.ValueLayout.*;
import static java.util.concurrent.TimeUnit.SECONDS;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Threads(1)
public class MdWalkerBenchmark {

    private static final int MADV_SEQUENTIAL = 2;
    private static final Linker LINKER = Linker.nativeLinker();
    private static final StructLayout CAPTURE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle ERRNO_HANDLE =
            CAPTURE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("errno"));

    private static final MethodHandle MADVISE = LINKER.downcallHandle(
            LINKER.defaultLookup().find("madvise").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT),
            Linker.Option.captureCallState("errno"),
            Linker.Option.critical(false)
    );

    private Arena arena;
    private MemorySegment memorySegment;
    private MdEventSink sink;
    private ItchMdWalker walker;

    @Setup(Level.Trial)
    public void setup() throws Throwable {
        arena = Arena.global();
        try (FileChannel ch = FileChannel.open(TestUtils.SAMPLE_FILE, StandardOpenOption.READ)) {
            memorySegment = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size(), arena);
        }
        sink = new CountingSink();
        walker = new ItchMdWalker(sink);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
    }

    @Benchmark
    public long walk() {
        return walker.walk(memorySegment);
    }

    static final class CountingSink implements MdEventSink {
        long count = 0L;

        @Override
        public void onEvent(MdEvent order) {
            ++count;
        }
    }

    private static void adviseSequential(MemorySegment segment) throws Throwable {
        try (Arena capture = Arena.ofConfined()) {
            MemorySegment errnoSeg = capture.allocate(CAPTURE_LAYOUT);
            int rc = (int) MADVISE.invokeExact(errnoSeg, segment, segment.byteSize(), MADV_SEQUENTIAL);
            if (rc != 0) {
                int errno = (int) ERRNO_HANDLE.get(errnoSeg, 0L);
                throw new IllegalStateException("madvise failed, rc=" + rc + ", errno=" + errno);
            }
        }
    }

}
