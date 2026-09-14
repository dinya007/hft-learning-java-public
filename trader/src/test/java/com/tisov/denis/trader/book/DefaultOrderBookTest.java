package com.tisov.denis.trader.book;

import com.tisov.denis.trader.book.side.BookSide;
import com.tisov.denis.trader.book.side.TreeMapBookSide;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import it.unimi.dsi.fastutil.Pair;

class DefaultOrderBookTest extends OrderBookTest {

    @Override
    public Pair<OrderBook, MdEventSink> orderBook(int capacity) {
        RestingOrderPool pool = new RestingOrderPool(capacity);
        BookSide bids = new TreeMapBookSide(Side.BID);
        BookSide asks = new TreeMapBookSide(Side.ASK);
        DefaultOrderBook book = new DefaultOrderBook(pool, bids, asks);
        return Pair.of(book, book);
    }
}
