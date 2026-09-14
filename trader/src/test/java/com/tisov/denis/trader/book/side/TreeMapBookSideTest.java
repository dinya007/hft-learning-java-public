package com.tisov.denis.trader.book.side;

public class TreeMapBookSideTest extends BookSideTest {
    @Override
    BookSide bookSide(byte side) {
        return new TreeMapBookSide(side);
    }
}
