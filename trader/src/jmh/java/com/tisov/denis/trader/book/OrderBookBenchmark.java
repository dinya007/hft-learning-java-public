package com.tisov.denis.trader.book;

import com.tisov.denis.trader.book.side.BookSide;
import com.tisov.denis.trader.book.side.FixedWindowLadderBookSide;
import com.tisov.denis.trader.book.side.Long2LongRBTreeMapBookSide;
import com.tisov.denis.trader.book.side.TreeMapBookSide;
import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
@Threads(1)
public class OrderBookBenchmark {

    private static final int BATCH = 1024;
    private static final int LIVE_WINDOW = 50_000;
    private static final long MID = 1_500_000;
    private static final long TICK = 100;
    private static final int HALF_SPAN = 100;
    private static final int POOL_CAP = 1 << 17;
    private static final long LADDER_MIN = MID - 1024 * TICK;
    private static final int LADDER_CAP = 2048;

    @Param({"ladder", "fastutilTree", "treemap"})
    public String impl;

    private MdEventSink sink;
    private OrderBook book;
    private final MdEvent ev = new MdEvent();

    private long[] liveRefs;
    private int ring;
    private long nextRef = 1;
    private final SplittableRandom rnd = new SplittableRandom(42);

    @Setup(Level.Trial)
    public void setup() {
        Object b = newBook(impl);
        this.book = (OrderBook) b;
        this.sink = (MdEventSink) b;
        this.liveRefs = new long[LIVE_WINDOW];
        for (int k = 0; k < LIVE_WINDOW; k++) {
            liveRefs[k] = addNew();
        }
        this.ring = 0;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void churn(Blackhole bh) {
        for (int j = 0; j < BATCH; j++) {
            sink.onEvent(ev.asDelete(0L, 1, liveRefs[ring]));
            liveRefs[ring] = addNew();

            if ((j & 3) == 0) {
                long ref = liveRefs[rnd.nextInt(LIVE_WINDOW)];
                sink.onEvent(ev.asExecute(0L, 1, ref, 1, 0L));
            }
            ring = (ring + 1 == LIVE_WINDOW) ? 0 : ring + 1;
            bh.consume(book.bestBid() ^ book.bestAsk());
        }
    }

    private long addNew() {
        long ref = nextRef++;
        boolean bid = (ref & 1) == 0;
        byte side = bid ? Side.BID : Side.ASK;
        long price = bid
                ? MID - (1 + rnd.nextInt(HALF_SPAN)) * TICK
                : MID + (1 + rnd.nextInt(HALF_SPAN)) * TICK;
        sink.onEvent(ev.asAdd(0L, 1, 0L, ref, side, 200, price));
        return ref;
    }

    private static Object newBook(String impl) {
        RestingOrderPool pool = new RestingOrderPool(POOL_CAP);
        BookSide bids, asks;
        switch (impl) {
            case "ladder" -> {
                bids = new FixedWindowLadderBookSide(Side.BID, LADDER_MIN, TICK, LADDER_CAP);
                asks = new FixedWindowLadderBookSide(Side.ASK, LADDER_MIN, TICK, LADDER_CAP);
            }
            case "fastutilTree" -> {
                bids = new Long2LongRBTreeMapBookSide(Side.BID);
                asks = new Long2LongRBTreeMapBookSide(Side.ASK);
            }
            case "treemap" -> {
                bids = new TreeMapBookSide(Side.BID);
                asks = new TreeMapBookSide(Side.ASK);
            }
            default -> throw new IllegalArgumentException(impl);
        }
        return new DefaultOrderBook(pool, bids, asks);
    }
}
