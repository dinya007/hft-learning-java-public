package com.tisov.denis.e2e;

import com.tisov.denis.trader.book.DefaultOrderBook;
import com.tisov.denis.trader.book.RestingOrderPool;
import com.tisov.denis.trader.book.side.WindowedLadderBookSide;
import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.signal.OrderBookImbalanceSignal;
import com.tisov.denis.trader.sink.MdEventSink;
import com.tisov.denis.trader.strategy.MarketMakerStrategy;
import com.tisov.denis.trader.strategy.OrderIntentSink;
import com.tisov.denis.trader.strategy.TradingPipeline;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Replay {

    private Replay() {
    }

    public static Pipeline build(TickerConfig config, OrderIntentSink intents) {
        OrderBookConfig ob = config.orderBookConfig();
        DefaultOrderBook book = new DefaultOrderBook(
                new RestingOrderPool(ob.poolCapacity()),
                new WindowedLadderBookSide(Side.BID, ob.minTrackingPrice(), ob.tickSize(), ob.priceLevels(), ob.overflowCapacity()),
                new WindowedLadderBookSide(Side.ASK, ob.minTrackingPrice(), ob.tickSize(), ob.priceLevels(), ob.overflowCapacity())
        );

        MarketMakerStrategy strategy = getStrategy(config, intents, ob);

        return new Pipeline(new TradingPipeline(book, book, strategy), book);
    }

    public static MemorySegment map(Path file) throws IOException {
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            return ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size(), Arena.global());
        }
    }

    public static final class SingleSymbolFilter implements MdEventSink {

        private final int locate;

        private final MdEventSink downstream;

        public SingleSymbolFilter(int locate, MdEventSink downstream) {
            this.locate = locate;
            this.downstream = downstream;
        }

        @Override
        public void onEvent(MdEvent e) {
            if (e.locate() == locate) {
                downstream.onEvent(e);
            }
        }

    }

    private static MarketMakerStrategy getStrategy(TickerConfig config, OrderIntentSink intents, OrderBookConfig orderBookConfig) {
        StrategyConfig strat = config.strategyConfig();
        MarketMakerStrategy mm = new MarketMakerStrategy(
                new OrderBookImbalanceSignal(1, 4),
                intents, config.locate(), strat.halfSpread(), strat.skew(), orderBookConfig.tickSize(), strat.qty()
        );
        return mm;
    }
}
