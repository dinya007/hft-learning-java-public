package com.tisov.denis.trader.book;

import com.tisov.denis.trader.book.side.BookSide;
import com.tisov.denis.trader.book.side.TreeMapBookSide;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import it.unimi.dsi.fastutil.Pair;

import org.junit.jupiter.api.Disabled;

@Disabled("Requires data/lobster/LOBSTER_SampleFile_MSFT_2012-06-21_10 (~198 MB CSV sample, not in the repo)")
public class DefaultLobsterOrderBookTest extends LobsterOrderBookTest {

    @Override
    public Pair<OrderBook, MdEventSink> orderBook(int capacity) {
        RestingOrderPool pool = new RestingOrderPool(capacity);
        BookSide bids = new TreeMapBookSide(Side.BID);
        BookSide asks = new TreeMapBookSide(Side.ASK);
        DefaultOrderBook book = new DefaultOrderBook(pool, bids, asks);
        return Pair.of(book, book);
    }
}
