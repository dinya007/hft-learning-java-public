package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.book.OrderBook;
import com.tisov.denis.trader.domain.Side;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

public final class WindowedLadderBookSide implements BookSide {

    private static final long TICK_SIZE = 100L;
    private static final int OUT_OF_WINDOW_INDEX = -1;
    private static final float OVERFLOW_FILL_FACTOR = 0.65f;

    private final long[] levels;
    private final Long2LongOpenHashMap overflowLevels;
    private final long minPriceIncl;
    private final long maxPriceExcl;
    private final byte side;

    private int bestIndex = -1;
    private int levelCount = 0;

    public WindowedLadderBookSide(byte side, long minPriceIncl, long tickSize, int capacity, int overflowCapacity) {
        if (tickSize != TICK_SIZE || minPriceIncl < 10_000L) {
            throw new IllegalArgumentException(
                    "This ladder supports only prices ≥ $1.00 with $0.01 tick: tickSize=" + tickSize
                    + ", minPriceIncl=" + minPriceIncl);
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        if (minPriceIncl % TICK_SIZE != 0L) {
            throw new IllegalArgumentException(
                    "minPriceIncl must be tick-aligned (a multiple of " + TICK_SIZE + "): " + minPriceIncl);
        }

        this.levels = new long[capacity];
        this.overflowLevels = new Long2LongOpenHashMap(overflowCapacity, OVERFLOW_FILL_FACTOR);
        this.overflowLevels.defaultReturnValue(0L);
        this.minPriceIncl = minPriceIncl;
        this.maxPriceExcl = minPriceIncl + TICK_SIZE * capacity;
        this.side = side;
    }

    @Override
    public void add(long price, long shares) {
        int index = indexOf(price);
        if (OUT_OF_WINDOW_INDEX == index) {
            addToOverflow(price, shares);
        } else {
            addToLevels(shares, index);
        }
    }

    @Override
    public void remove(long price, long shares) {
        int index = indexOf(price);
        if (OUT_OF_WINDOW_INDEX == index) {
            removeFromOverflow(price, shares);
        } else {
            removeFromLevels(price, shares, index);
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
        return index == OUT_OF_WINDOW_INDEX ? overflowLevels.get(price) : levels[index];
    }

    private void addToOverflow(long price, long shares) {
        overflowLevels.addTo(price, shares);
    }

    private void addToLevels(long shares, int index) {
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

    private void removeFromLevels(long price, long shares, int index) {
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

    private void removeFromOverflow(long price, long shares) {
        long before = overflowLevels.get(price);
        long after = before - shares;
        assert after >= 0 : "over-drain: feed gap or bug @ " + price;
        if (after == 0) {
            overflowLevels.remove(price);
        } else {
            overflowLevels.put(price, after);
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

    private boolean improvesBest(int index) {
        return Side.BID == side
                ? bestIndex < index
                : bestIndex == -1 || bestIndex > index;
    }

    private int indexOf(long price) {
        if (price < minPriceIncl || price >= maxPriceExcl) {
            return OUT_OF_WINDOW_INDEX;
        }
        return (int) ((price - minPriceIncl) / TICK_SIZE);
    }

    private long priceAt(int index) {
        return minPriceIncl + index * TICK_SIZE;
    }
}
