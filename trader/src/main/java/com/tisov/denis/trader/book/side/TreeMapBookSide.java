package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.domain.Side;

import java.util.Comparator;
import java.util.TreeMap;

import static com.tisov.denis.trader.book.OrderBook.NO_QUOTE;

public final class TreeMapBookSide implements BookSide {

    private final TreeMap<Long, Long> levels;

    public TreeMapBookSide(byte side) {
        this.levels = Side.BID == side
                ? new TreeMap<>(Comparator.reverseOrder())
                : new TreeMap<>();
    }

    @Override
    public void add(long price, long shares) {
        levels.merge(price, shares, Long::sum);
    }

    @Override
    public void remove(long price, long shares) {
        Long current = levels.get(price);
        if (current == null) {
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
        return levels.isEmpty() ? NO_QUOTE : levels.firstKey();
    }

    @Override
    public long bestShares() {
        return levels.isEmpty() ? 0 : levels.firstEntry().getValue();

    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    public int levelCount() {
        return levels.size();
    }

    public long sharesAt(long price) {
        Long shares = levels.get(price);
        return shares == null ? 0 : shares;
    }

}
