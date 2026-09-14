package com.tisov.denis.e2e;

import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import org.openjdk.jmh.annotations.*;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 2, timeUnit = SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = SECONDS)
@Threads(1)
public class PipelineChurnBenchmark {

    public enum Eviction {
        FIFO,
        RANDOM
    }

    private static final TickerConfig TICKER = Tickers.AMD;
    private static final OrderBookConfig OB = TICKER.orderBookConfig();

    private static final int BATCH = 1024;
    private static final int LIVE_WINDOW = OB.maxOrders();
    private static final long MID = OB.minTrackingPrice() + OB.priceLevels() * OB.tickSize() / 2;
    private static final long HALF_SPAN_TICKS = OB.priceLevels() / 2 - 1;

    @Param({"FIFO", "RANDOM"})
    Eviction eviction;

    private MdEventSink pipeline;
    private final MdEvent ev = new MdEvent();
    private long[] liveOrderIds;
    private int current;
    private long nextId = 1;
    private final SplittableRandom rnd = new SplittableRandom(42);
    private long result;

    @Setup(Level.Trial)
    public void setup() {
        pipeline = Replay.build(TICKER, i -> result += i.price()).sink();
        liveOrderIds = new long[LIVE_WINDOW];
        for (int k = 0; k < LIVE_WINDOW; k++) {
            liveOrderIds[k] = addNew();
        }
        current = 0;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public long churn() {
        if (eviction == Eviction.RANDOM) {
            churnRandom();
        } else {
            churnFifo();
        }
        return result;
    }

    private void churnFifo() {
        int cur = current;
        for (int j = 0; j < BATCH; j++) {
            delete(liveOrderIds[cur]);
            liveOrderIds[cur] = addNew();
            cur = (cur + 1 == LIVE_WINDOW) ? 0 : cur + 1;
        }
        current = cur;
    }

    private void churnRandom() {
        for (int j = 0; j < BATCH; j++) {
            int slot = rnd.nextInt(LIVE_WINDOW);
            delete(liveOrderIds[slot]);
            liveOrderIds[slot] = addNew();
        }
    }

    private void delete(long ref) {
        pipeline.onEvent(ev.asDelete(0L, TICKER.locate(), ref));
    }

    private long addNew() {
        long id = nextId++;
        boolean bid = (id & 1) == 0;
        byte side = bid ? Side.BID : Side.ASK;
        long price = bid
                ? MID - (1 + rnd.nextLong(HALF_SPAN_TICKS)) * OB.tickSize()
                : MID + (1 + rnd.nextLong(HALF_SPAN_TICKS)) * OB.tickSize();
        pipeline.onEvent(ev.asAdd(0L, TICKER.locate(), 0L, id, side, 200, price));
        return id;
    }

}
