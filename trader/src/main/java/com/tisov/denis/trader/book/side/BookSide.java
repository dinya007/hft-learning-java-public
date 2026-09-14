package com.tisov.denis.trader.book.side;

public interface BookSide {

    void add(long price, long shares);

    void remove(long price, long shares);

    long bestPrice();

    long bestShares();

    boolean isEmpty();

    int levelCount();

    long sharesAt(long price);

}
