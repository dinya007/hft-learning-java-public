package com.tisov.denis.trader.book;

public interface OrderBook {

    long NO_QUOTE = Long.MIN_VALUE;

    long bestBid();

    long bestAsk();

    long bestBidShares();

    long bestAskShares();

    long spread();

    int liveOrders();

}
