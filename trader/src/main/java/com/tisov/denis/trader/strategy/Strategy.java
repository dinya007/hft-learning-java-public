package com.tisov.denis.trader.strategy;

import com.tisov.denis.trader.book.OrderBook;

public interface Strategy {

    void onBookUpdate(OrderBook orderBook, long timestamp);

}
