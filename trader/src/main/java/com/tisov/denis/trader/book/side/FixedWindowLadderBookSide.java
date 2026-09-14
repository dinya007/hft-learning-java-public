package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.book.OrderBook;
import com.tisov.denis.trader.domain.Side;

public final class FixedWindowLadderBookSide implements BookSide {

    private static final long TICK_SIZE = 100L;
    private static final int IGNORE_PRICE_INDEX = -1;

    private final long[] levels;
    private final long minPriceIncl;
    private final long maxPriceExcl;
    private final byte side;

    private int bestIndex = -1;
    private int levelCount = 0;

    public FixedWindowLadderBookSide(byte side, long minPriceIncl, long tickSize, int levels) {
        if (tickSize != TICK_SIZE || minPriceIncl < 10_000L) {
            throw new IllegalArgumentException(
                    "This ladder supports only prices ≥ $1.00 with $0.01 tick: tickSize=" + tickSize
                    + ", minPriceIncl=" + minPriceIncl);
        }
        if (levels <= 0) {
            throw new IllegalArgumentException("levels must be positive: " + levels);
        }
        if (minPriceIncl % TICK_SIZE != 0L) {
            throw new IllegalArgumentException(
                    "minPriceIncl must be tick-aligned (a multiple of " + TICK_SIZE + "): " + minPriceIncl);
        }

        this.levels = new long[levels];
        this.minPriceIncl = minPriceIncl;
        this.maxPriceExcl = minPriceIncl + TICK_SIZE * levels;
        this.side = side;
    }

    @Override
    public void add(long price, long shares) {
        int index = indexOf(price);
        if (IGNORE_PRICE_INDEX == index) {
            return;
        }

        long before = levels[index];
        long after = before + shares;
        levels[index] = after;

        boolean levelActivated = before <= 0 && after > 0;
        if (levelActivated) {
            ++levelCount;

            if (improvesBest(index)) {
                bestIndex = index;
            }
        }
    }

    @Override
    public void remove(long price, long shares) {
        int index = indexOf(price);
        if (IGNORE_PRICE_INDEX == index) {
            return;
        }
        long before = levels[index];
        long after = before - shares;

        assert after >= 0 : "over-drain: feed gap or bug @ " + price;

        levels[index] = after;

        boolean levelDeactivated = before > 0 && after == 0;
        if (levelDeactivated) {
            --levelCount;
            if (bestIndex == index) {
                moveBestAfterDeactivate(index);
            }
        }
    }

    private void moveBestAfterDeactivate(int deactivatedIndex) {
        if (levelCount == 0) {
            bestIndex = -1;
            return;
        }

        if (Side.BID == side) {
            for (int i = deactivatedIndex - 1; i >= 0; i--) {
                if (levels[i] > 0) {
                    bestIndex = i;
                    return;
                }
            }
        } else {
            for (int i = deactivatedIndex + 1; i < levels.length; i++) {
                if (levels[i] > 0) {
                    bestIndex = i;
                    return;
                }
            }
        }
    }

    @Override
    public long bestPrice() {
        return isEmpty() ? OrderBook.NO_QUOTE : priceAt(bestIndex);
    }

    @Override
    public long bestShares() {
        return isEmpty() ? 0 : levels[bestIndex];
    }

    @Override
    public boolean isEmpty() {
        return levelCount == 0;
    }

    @Override
    public int levelCount() {
        return levelCount;
    }

    @Override
    public long sharesAt(long price) {
        int index = indexOf(price);
        return index == IGNORE_PRICE_INDEX ? 0 : levels[index];
    }

    private boolean improvesBest(int index) {
        return Side.BID == side
                ? bestIndex < index
                : bestIndex == -1 || bestIndex > index;
    }

    private int indexOf(long price) {
        if (price < minPriceIncl || price >= maxPriceExcl) {
            return IGNORE_PRICE_INDEX;
        }
        return (int) ((price - minPriceIncl) / TICK_SIZE);
    }

    private long priceAt(int index) {
        return minPriceIncl + index * TICK_SIZE;
    }
}
