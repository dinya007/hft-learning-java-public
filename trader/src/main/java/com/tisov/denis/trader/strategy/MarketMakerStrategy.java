package com.tisov.denis.trader.strategy;

import com.tisov.denis.trader.book.OrderBook;
import com.tisov.denis.trader.domain.OrderIntent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.id.OrderIdGenerator;
import com.tisov.denis.trader.signal.Signal;
import com.tisov.denis.trader.signal.SignalSide;

import static com.tisov.denis.trader.book.OrderBook.NO_QUOTE;

public class MarketMakerStrategy implements Strategy {

    private final Signal signal;
    private final OrderIntentSink orderIntentSink;
    private final int symbolId;
    private final long halfSpread;
    private final long skew;
    private final long tick;
    private final int quoteQuantity;
    private final OrderIntent orderIntent = new OrderIntent();

    private long position;
    private long lastBidPrice;
    private long lastAskPrice;

    public MarketMakerStrategy(Signal signal, OrderIntentSink orderIntentSink, int symbolId, long halfSpread, long skew, long tick, int quoteQuantity) {
        assert halfSpread > 0;
        assert skew > 0;
        assert tick > 0;
        assert quoteQuantity > 0;

        this.signal = signal;
        this.orderIntentSink = orderIntentSink;
        this.symbolId = symbolId;
        this.halfSpread = halfSpread;
        this.skew = skew;
        this.tick = tick;
        this.quoteQuantity = quoteQuantity;
    }

    @Override
    public void onBookUpdate(OrderBook orderBook, long timestamp) {
        long bestBid = orderBook.bestBid();
        long bestAsk = orderBook.bestAsk();

        if (bestBid == NO_QUOTE || bestAsk == NO_QUOTE) {
            pullQuotes();
            return;
        }

        byte sig = signal.get(orderBook.bestBidShares(), orderBook.bestAskShares());
        long middle = (bestBid + bestAsk) / 2;
        long totalSkew = getTotalSkew(sig);
        long bidPrice = floorToTick(middle - halfSpread + totalSkew);
        long askPrice = floorToTick(middle + halfSpread + totalSkew);

        emitIntentIfChanged(Side.BID, bidPrice, timestamp);
        emitIntentIfChanged(Side.ASK, askPrice, timestamp);
    }

    private long getTotalSkew(byte sig) {
        long sigSkew = inventorySkew();
        if (SignalSide.UP == sig) {
            sigSkew += skew;
        } else if (SignalSide.DOWN == sig) {
            sigSkew -= skew;
        } else {
            sigSkew = 0;
        }
        return sigSkew;
    }

    private void pullQuotes() {
        lastBidPrice = NO_QUOTE;
        lastAskPrice = NO_QUOTE;
    }

    private long inventorySkew() {
        return 0;
    }

    private long floorToTick(long price) {
        return price - (price % tick);
    }

    private long ceilToTick(long price) {
        return price - (price % tick) + tick;
    }

    private void emitIntentIfChanged(byte side, long price, long timestamp) {
        if (Side.BID == side) {
            if (lastBidPrice == price) {
                return;
            }
            lastBidPrice = price;
        } else {
            if (lastAskPrice == price) {
                return;
            }
            lastAskPrice = price;
        }
        orderIntentSink.onIntent(
                orderIntent.asLimit(timestamp, OrderIdGenerator.getNextMarketMakerStrategyId(), symbolId, side, price, quoteQuantity)
        );
    }

}
