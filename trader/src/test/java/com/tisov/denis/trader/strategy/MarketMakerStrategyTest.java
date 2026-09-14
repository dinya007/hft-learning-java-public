package com.tisov.denis.trader.strategy;

import com.tisov.denis.trader.book.OrderBook;
import com.tisov.denis.trader.domain.OrderIntent;
import com.tisov.denis.trader.domain.OrderType;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.id.OrderIdGenerator;
import com.tisov.denis.trader.signal.OrderBookImbalanceSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerStrategyTest {

    private static final long TICK = 100;
    private static final long HALF_SPREAD = 200;
    private static final long SKEW = 100;
    private static final int QTY = 100;
    private static final int SYMBOL = 7;

    private StubOrderBook book;
    private CaptureSink sink;
    private MarketMakerStrategy strategy;

    @BeforeEach
    void setUp() {
        OrderIdGenerator.reset();
        book = new StubOrderBook();
        sink = new CaptureSink();
        strategy = new MarketMakerStrategy(
                new OrderBookImbalanceSignal(1, 4), sink, SYMBOL,
                HALF_SPREAD, SKEW, TICK, QTY
        );
    }

    @Test
    void emitsBidAndAskOnTwoSidedBook() {
        book.set(99_9900, 100_0100, 100, 100);

        strategy.onBookUpdate(book, 42L);

        assertThat(sink.quotes).hasSize(2);
        Quote bid = sink.side(Side.BID);
        Quote ask = sink.side(Side.ASK);
        assertThat(bid.price()).isEqualTo(99_9800);
        assertThat(ask.price()).isEqualTo(100_0200);
        assertThat(bid.qty()).isEqualTo(QTY);
        assertThat(bid.symbolId()).isEqualTo(SYMBOL);
        assertThat(bid.ordType()).isEqualTo(OrderType.LIMIT);
        assertThat(bid.ts()).isEqualTo(42L);
    }

    @Test
    void upSignalSkewsQuotesUp() {
        book.set(99_9900, 100_0100, 900, 100);

        strategy.onBookUpdate(book, 1L);

        assertThat(sink.side(Side.BID).price()).isEqualTo(99_9900);
        assertThat(sink.side(Side.ASK).price()).isEqualTo(100_0300);
    }

    @Test
    void downSignalSkewsQuotesDown() {
        book.set(99_9900, 100_0100, 100, 900);

        strategy.onBookUpdate(book, 1L);

        assertThat(sink.side(Side.BID).price()).isEqualTo(99_9700);
        assertThat(sink.side(Side.ASK).price()).isEqualTo(100_0100);
    }

    @Test
    void dedupSuppressesUnchangedRequote() {
        book.set(99_9900, 100_0100, 100, 100);
        strategy.onBookUpdate(book, 1L);
        sink.quotes.clear();

        strategy.onBookUpdate(book, 2L);

        assertThat(sink.quotes).isEmpty();
    }

    @Test
    void requotesOnlyChangedSide() {
        book.set(99_9900, 100_0100, 100, 100);
        strategy.onBookUpdate(book, 1L);
        sink.quotes.clear();

        book.set(99_9900, 100_0300, 100, 100);
        strategy.onBookUpdate(book, 2L);

        assertThat(sink.quotes).isNotEmpty();
    }

    @Test
    void oneSidedBookPullsQuotes() {
        book.set(99_9900, Long.MIN_VALUE, 100, 0);

        strategy.onBookUpdate(book, 1L);

        assertThat(sink.quotes).isEmpty();
    }

    @Test
    void pullThenRequoteAfterReset() {
        book.set(99_9900, 100_0100, 100, 100);
        strategy.onBookUpdate(book, 1L);
        sink.quotes.clear();

        book.set(99_9900, Long.MIN_VALUE, 100, 0);
        strategy.onBookUpdate(book, 2L);

        book.set(99_9900, 100_0100, 100, 100);
        strategy.onBookUpdate(book, 3L);

        assertThat(sink.quotes).hasSize(2);
    }

    @Test
    void quotesAreTickAligned() {
        book.set(99_9950, 100_0050, 100, 100);

        strategy.onBookUpdate(book, 1L);

        assertThat(sink.side(Side.BID).price() % TICK).isZero();
        assertThat(sink.side(Side.ASK).price() % TICK).isZero();
    }

    @Test
    void clOrdIdIncrementsPerEmission() {
        book.set(99_9900, 100_0100, 100, 100);
        strategy.onBookUpdate(book, 1L);

        assertThat(sink.side(Side.BID).clOrdId()).isEqualTo(0L);
        assertThat(sink.side(Side.ASK).clOrdId()).isEqualTo(1L);
    }

    private record Quote(long ts, long clOrdId, int symbolId, byte side, byte ordType,
                         long price, int qty) {
    }

    private static final class CaptureSink implements OrderIntentSink {
        final List<Quote> quotes = new ArrayList<>();

        @Override
        public void onIntent(OrderIntent i) {
            quotes.add(new Quote(i.timestamp(), i.clientOrderId(), i.symbolId(), i.side(), i.orderType(),
                    i.price(), i.quantity()));
        }

        Quote side(byte side) {
            return quotes.stream().filter(q -> q.side() == side).findFirst().orElseThrow();
        }
    }

    private static final class StubOrderBook implements OrderBook {
        private long bestBid = Long.MIN_VALUE;
        private long bestAsk = Long.MIN_VALUE;
        private long bestBidShares;
        private long bestAskShares;

        void set(long bestBid, long bestAsk, long bidShares, long askShares) {
            this.bestBid = bestBid;
            this.bestAsk = bestAsk;
            this.bestBidShares = bidShares;
            this.bestAskShares = askShares;
        }

        @Override
        public long bestBid() {
            return bestBid;
        }

        @Override
        public long bestAsk() {
            return bestAsk;
        }

        @Override
        public long bestBidShares() {
            return bestBidShares;
        }

        @Override
        public long bestAskShares() {
            return bestAskShares;
        }

        @Override
        public long spread() {
            return bestAsk - bestBid;
        }

        @Override
        public int liveOrders() {
            return 0;
        }
    }

}
