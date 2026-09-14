package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.domain.Side;
import it.unimi.dsi.fastutil.longs.Long2LongRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongComparators;

import static com.tisov.denis.trader.book.OrderBook.NO_QUOTE;

public final class Long2LongRBTreeMapBookSide implements BookSide {

    private final Long2LongRBTreeMap levels;
    private final long nullValue = 0;

    public Long2LongRBTreeMapBookSide(byte side) {
        this.levels = Side.BID == side
                ? new Long2LongRBTreeMap(LongComparators.OPPOSITE_COMPARATOR)
                : new Long2LongRBTreeMap();
        this.levels.defaultReturnValue(nullValue);
    }

    @Override
    public void add(long price, long shares) {
        levels.mergeLong(price, shares, Long::sum);
    }

    @Override
    public void remove(long price, long shares) {
        long current = levels.get(price);
        if (current == nullValue) {
            return;
        }

        long remaining = current - shares;

        if (remaining <= 0) {
            levels.remove(price);
        } else {
            levels.put(price, remaining);
        }
    }

    @Override
    public long bestPrice() {
        return levels.isEmpty() ? NO_QUOTE : levels.firstLongKey();
    }

    @Override
    public long bestShares() {
        return levels.isEmpty() ? 0 : levels.get(levels.firstLongKey());
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    public int levelCount() {
        return levels.size();
    }

    public long sharesAt(long price) {
        return levels.get(price);
    }
}
