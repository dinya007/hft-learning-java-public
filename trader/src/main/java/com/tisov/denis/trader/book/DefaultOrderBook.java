package com.tisov.denis.trader.book;

import com.tisov.denis.trader.book.side.BookSide;
import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import static com.tisov.denis.trader.domain.MdEvent.*;

public class DefaultOrderBook implements MdEventSink, OrderBook {

    private static final int MISSING_VALUE = -1;
    private static final float LOAD_FACTOR = 0.65f;

    private final RestingOrderPool restingOrderPool;
    private final Long2IntOpenHashMap orderPoolIndexByOrderRef;
    private final BookSide[] sides;

    public DefaultOrderBook(RestingOrderPool restingOrderPool, BookSide bidBookSide, BookSide askBookSide) {
        this.restingOrderPool = restingOrderPool;
        this.sides = new BookSide[]{bidBookSide, askBookSide};
        this.orderPoolIndexByOrderRef = new Long2IntOpenHashMap(restingOrderPool.capacity(), LOAD_FACTOR);
        this.orderPoolIndexByOrderRef.defaultReturnValue(MISSING_VALUE);
    }

    @Override
    public void onEvent(MdEvent event) {
        switch (event.type()) {
            case TYPE_ADD -> add(event);
            case TYPE_EXECUTE, TYPE_EXECUTE_PRICE, TYPE_CANCEL -> reduce(event.orderRef(), event.shares());
            case TYPE_REPLACE -> replace(event);
            case TYPE_DELETE -> delete(event.orderRef());
            default -> throw new IllegalArgumentException("Not supported event type %s".formatted(event.type()));
        }
    }

    @Override
    public long bestBid() {
        return sides[Side.BID].bestPrice();
    }

    @Override
    public long bestAsk() {
        return sides[Side.ASK].bestPrice();
    }

    @Override
    public long bestBidShares() {
        return sides[Side.BID].bestShares();
    }

    @Override
    public long bestAskShares() {
        return sides[Side.ASK].bestShares();
    }

    @Override
    public long spread() {
        long bidPrice = sides[Side.BID].bestPrice();
        long askPrice = sides[Side.ASK].bestPrice();
        if (NO_QUOTE == bidPrice || NO_QUOTE == askPrice) {
            return NO_QUOTE;
        } else {
            return askPrice - bidPrice;
        }
    }

    @Override
    public int liveOrders() {
        return restingOrderPool.inUse();
    }

    private void add(MdEvent event) {
        int index = restingOrderPool.acquire();
        long orderRef = event.orderRef();
        long price = event.price();
        long shares = event.shares();
        byte side = event.side();
        restingOrderPool.set(index, orderRef, price, shares, event.timestamp(), event.stock(), event.locate(), side);
        orderPoolIndexByOrderRef.put(orderRef, index);
        sides[side].add(price, shares);
    }

    private void reduce(long orderRef, long shares) {
        int index = indexOf(orderRef);

        long remainingShares = restingOrderPool.reduceShares(index, shares);
        sides[restingOrderPool.side(index)].remove(restingOrderPool.price(index), shares);

        if (remainingShares == 0) {
            orderPoolIndexByOrderRef.remove(orderRef);
            restingOrderPool.release(index);
        }
    }

    private void replace(MdEvent event) {
        long orderRef = event.orderRef();
        long newOrderRef = event.newOrderRef();
        int index = indexOf(orderRef);
        long previousPrice = restingOrderPool.price(index);
        long previousShares = restingOrderPool.shares(index);
        long stock = restingOrderPool.stock(index);
        byte side = restingOrderPool.side(index);
        long newPrice = event.price();
        long newShares = event.shares();

        orderPoolIndexByOrderRef.remove(orderRef);
        orderPoolIndexByOrderRef.put(newOrderRef, index);

        sides[side].remove(previousPrice, previousShares);
        sides[side].add(newPrice, newShares);

        restingOrderPool.set(index, newOrderRef, newPrice, newShares, event.timestamp(), stock, event.locate(), side);
    }

    private void delete(long orderRef) {
        int index = indexOf(orderRef);
        sides[restingOrderPool.side(index)].remove(restingOrderPool.price(index), restingOrderPool.shares(index));
        orderPoolIndexByOrderRef.remove(orderRef);
        restingOrderPool.release(index);
    }

    private int indexOf(long orderRef) {
        int index = orderPoolIndexByOrderRef.get(orderRef);
        assert MISSING_VALUE != index : "Not known order %s".formatted(orderRef);
        return index;
    }
}
