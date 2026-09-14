package com.tisov.denis.trader.book.side;

public class Long2LongRBTreeMapBookSideTest extends BookSideTest {

    @Override
    BookSide bookSide(byte side) {
        return new Long2LongRBTreeMapBookSide(side);
    }
}
