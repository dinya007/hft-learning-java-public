package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.domain.Side;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BookSideTest {

    private static final int PRICE_100 = 100_0000;
    private static final int PRICE_101 = 101_0000;
    private static final int PRICE_102 = 102_0000;

    abstract BookSide bookSide(byte side);

    @Test
    void levelRemovedWhenFullyDrained() {
        BookSide bids = bookSide(Side.BID);
        bids.add(PRICE_100, 500);

        bids.remove(PRICE_100, 500);

        assertThat(bids.isEmpty()).isTrue();
        assertThat(bids.bestPrice()).isEqualTo(Long.MIN_VALUE);
        assertThat(bids.levelCount()).isZero();
    }

    @Test
    void partialRemoveKeepsLevel() {
        BookSide asks = bookSide(Side.ASK);
        asks.add(PRICE_100, 500);

        asks.remove(PRICE_100, 200);

        assertThat(asks.sharesAt(PRICE_100)).isEqualTo(300);
        assertThat(asks.bestPrice()).isEqualTo(PRICE_100);
    }

    @Test
    void bestIsHighestForBids() {
        BookSide bids = bookSide(Side.BID);

        bids.add(PRICE_100, 10);
        bids.add(PRICE_102, 10);
        bids.add(PRICE_101, 10);

        assertThat(bids.bestPrice()).isEqualTo(PRICE_102);
    }

    @Test
    void bestIsLowestForAsks() {
        BookSide asks = bookSide(Side.ASK);

        asks.add(PRICE_100, 10);
        asks.add(PRICE_102, 10);
        asks.add(PRICE_101, 10);

        assertThat(asks.bestPrice()).isEqualTo(PRICE_100);
    }

    @Test
    void levelRemovedWhenFullyDrainedAsk() {
        BookSide asks = bookSide(Side.ASK);
        asks.add(PRICE_100, 500);

        asks.remove(PRICE_100, 500);

        assertThat(asks.isEmpty()).isTrue();
        assertThat(asks.bestPrice()).isEqualTo(Long.MIN_VALUE);
        assertThat(asks.levelCount()).isZero();
    }

    @Test
    void partialRemoveKeepsLevelBid() {
        BookSide bids = bookSide(Side.BID);
        bids.add(PRICE_100, 500);

        bids.remove(PRICE_100, 200);

        assertThat(bids.sharesAt(PRICE_100)).isEqualTo(300);
        assertThat(bids.bestPrice()).isEqualTo(PRICE_100);
    }

    @Test
    void bestBidMovesDownAfterDrainOfBestLevel() {
        BookSide bids = bookSide(Side.BID);
        bids.add(PRICE_100, 10);
        bids.add(PRICE_101, 10);
        bids.add(PRICE_102, 10);

        bids.remove(PRICE_102, 10);

        assertThat(bids.bestPrice()).isEqualTo(PRICE_101);
        assertThat(bids.levelCount()).isEqualTo(2);
    }

    @Test
    void bestAskMovesUpAfterDrainOfBestLevel() {
        BookSide asks = bookSide(Side.ASK);
        asks.add(PRICE_100, 10);
        asks.add(PRICE_101, 10);
        asks.add(PRICE_102, 10);

        asks.remove(PRICE_100, 10);

        assertThat(asks.bestPrice()).isEqualTo(PRICE_101);
        assertThat(asks.levelCount()).isEqualTo(2);
    }

    @Test
    void drainNonBestLevelDoesNotChangeBestBid() {
        BookSide bids = bookSide(Side.BID);
        bids.add(PRICE_100, 10);
        bids.add(PRICE_101, 10);
        bids.add(PRICE_102, 10);

        bids.remove(PRICE_100, 10);

        assertThat(bids.bestPrice()).isEqualTo(PRICE_102);
        assertThat(bids.levelCount()).isEqualTo(2);
    }

    @Test
    void drainNonBestLevelDoesNotChangeBestAsk() {
        BookSide asks = bookSide(Side.ASK);
        asks.add(PRICE_100, 10);
        asks.add(PRICE_101, 10);
        asks.add(PRICE_102, 10);

        asks.remove(PRICE_102, 10);

        assertThat(asks.bestPrice()).isEqualTo(PRICE_100);
        assertThat(asks.levelCount()).isEqualTo(2);
    }

    @Test
    void sharesAccumulateOnExistingLevelBid() {
        BookSide bids = bookSide(Side.BID);
        bids.add(PRICE_100, 100);
        bids.add(PRICE_100, 200);

        assertThat(bids.sharesAt(PRICE_100)).isEqualTo(300);
        assertThat(bids.levelCount()).isEqualTo(1);
    }

    @Test
    void sharesAccumulateOnExistingLevelAsk() {
        BookSide asks = bookSide(Side.ASK);
        asks.add(PRICE_100, 100);
        asks.add(PRICE_100, 200);

        assertThat(asks.sharesAt(PRICE_100)).isEqualTo(300);
        assertThat(asks.levelCount()).isEqualTo(1);
    }

    @Test
    void bestSharesReflectsCurrentLevelBid() {
        BookSide bids = bookSide(Side.BID);
        bids.add(PRICE_100, 100);
        bids.add(PRICE_101, 300);

        assertThat(bids.bestShares()).isEqualTo(300);
    }

    @Test
    void bestSharesReflectsCurrentLevelAsk() {
        BookSide asks = bookSide(Side.ASK);
        asks.add(PRICE_100, 300);
        asks.add(PRICE_101, 100);

        assertThat(asks.bestShares()).isEqualTo(300);
    }

}
