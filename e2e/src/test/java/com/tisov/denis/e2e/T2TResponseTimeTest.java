package com.tisov.denis.e2e;

import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.sink.MdEventSink;
import com.tisov.denis.trader.walker.ItchMdWalker;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class T2TResponseTimeTest {

    private static final Path DATA = Path.of("..").toAbsolutePath().resolve("data");
    private static final TickerConfig TICKER = Tickers.AMD;
    private static final int LOCATE = TICKER.locate();
    private static final int WARMUP_PASSES = 10;

    @Test
    void peak5s() throws IOException {
        run(windowFile("peak_5s"), SECONDS.toNanos(57_597), SECONDS.toNanos(57_602), TICKER.ticker() + " peak 5s @15:59:57");
    }

    @Disabled("Real-time paced replay takes 1 min of wall-clock time")
    @Test
    void peakMinute() throws IOException {
        run(windowFile("peak_1m"), SECONDS.toNanos(53_019), SECONDS.toNanos(53_079), TICKER.ticker() + " peak 1m @14:43:39");
    }

    @Disabled("Real-time paced replay takes 5 min of wall-clock time")
    @Test
    void peak5Min() throws IOException {
        run(windowFile("peak_5m"), SECONDS.toNanos(52_927), SECONDS.toNanos(53_227), TICKER.ticker() + " peak 5m @14:42:07");
    }

    private static String windowFile(String window) {
        return TICKER.ticker() + "/01302020." + window + ".itch";
    }

    private void run(String file, long startNs, long endNs, String label) throws IOException {
        MemorySegment memorySegment = Replay.map(DATA.resolve(file));

        for (int i = 0; i < WARMUP_PASSES; i++) {
            MdEventSink p = Replay.build(TICKER, x -> {
            }).sink();
            new ItchMdWalker(e -> {
                if (e.locate() == LOCATE) p.onEvent(e);
            }).walk(memorySegment);
        }

        Histogram response = new Histogram(3);
        Histogram service = new Histogram(3);
        GatedPacedSink sink = new GatedPacedSink(Replay.build(TICKER, _ -> {
        }).sink(), startNs, endNs, response, service);
        try {
            new ItchMdWalker(sink).walk(memorySegment);
        } catch (Stop ignored) {
        }
        double wallMs = (System.nanoTime() - sink.t0) / 1e6;

        System.out.printf("%n%s  processed=%,d  wall=%.0f ms%n", label, sink.processed, wallMs);
        System.out.printf("  %-9s %9s %9s %9s %9s %9s %9s%n", "(ns)", "p50", "p90", "p99", "p99.9", "max", "mean");
        line("service", service);
        line("response", response);
        System.out.printf("  queueing (response − service):  p99=%,d  p99.9=%,d  max=%,d ns%n",
                response.getValueAtPercentile(99.0) - service.getValueAtPercentile(99.0),
                response.getValueAtPercentile(99.9) - service.getValueAtPercentile(99.9),
                response.getMaxValue() - service.getMaxValue());

        assertThat(sink.processed).isPositive();
    }

    private static void line(String name, Histogram h) {
        System.out.printf("  %-9s %,9d %,9d %,9d %,9d %,9d %,9.0f%n", name,
                h.getValueAtPercentile(50.0), h.getValueAtPercentile(90.0), h.getValueAtPercentile(99.0),
                h.getValueAtPercentile(99.9), h.getMaxValue(), h.getMean());
    }

    private static final class GatedPacedSink implements MdEventSink {
        private final MdEventSink pipeline;
        private final long startNs;
        private final long endNs;
        private final Histogram responseTimeHistogram;
        private final Histogram serviceTimeHistogram;
        long t0;
        long datasetBaseTs;
        long processed;
        boolean measuring;

        GatedPacedSink(MdEventSink pipeline, long startNs, long endNs, Histogram responseTimeHistogram, Histogram serviceTimeHistogram) {
            this.pipeline = pipeline;
            this.startNs = startNs;
            this.endNs = endNs;
            this.responseTimeHistogram = responseTimeHistogram;
            this.serviceTimeHistogram = serviceTimeHistogram;
        }

        @Override
        public void onEvent(MdEvent mdEvent) {
            if (mdEvent.locate() != LOCATE) {
                return;
            }
            long eventTimestamp = mdEvent.timestamp();
            if (eventTimestamp >= endNs) {
                throw STOP;
            }
            if (eventTimestamp < startNs) {
                pipeline.onEvent(mdEvent);
                return;
            }
            if (!measuring) {
                t0 = System.nanoTime();
                datasetBaseTs = eventTimestamp;
                measuring = true;
            }
            long scheduled = t0 + (eventTimestamp - datasetBaseTs);
            while (System.nanoTime() - scheduled < 0) {
                Thread.onSpinWait();
            }
            long start = System.nanoTime();
            pipeline.onEvent(mdEvent);
            long finish = System.nanoTime();
            long responseTime = finish - scheduled;
            long serviceTime = finish - start;
            responseTimeHistogram.recordValue(responseTime < 0 ? 0 : responseTime);
            serviceTimeHistogram.recordValue(serviceTime);
            processed++;
        }
    }

    private static final Stop STOP = new Stop();

    private static final class Stop extends RuntimeException {
        Stop() {
            super(null, null, false, false);
        }
    }
}
