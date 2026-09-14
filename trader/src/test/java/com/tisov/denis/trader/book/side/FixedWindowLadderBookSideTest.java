package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.domain.Side;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowLadderBookSideTest extends BookSideTest {

    @Override
    BookSide bookSide(byte side) {
        return new FixedWindowLadderBookSide(side, 100_0000, 1_00, 1024);
    }

    @Test
    void reciprocalExactAtMaxIndex() {
        long minPrice = 100_0000L;
        long topPrice = minPrice + 4095 * 100L;
        FixedWindowLadderBookSide bids = new FixedWindowLadderBookSide(Side.BID, minPrice, 100, 4096);

        bids.add(topPrice, 10);

        assertThat(bids.sharesAt(topPrice)).isEqualTo(10);
        assertThat(bids.bestPrice()).isEqualTo(topPrice);
    }

    @Test
    void ignoresAddBelowWindow() {
        BookSide bids = bookSide(Side.BID);

        bids.add(99_9900, 500);

        assertThat(bids.isEmpty()).isTrue();
        assertThat(bids.levelCount()).isZero();
        assertThat(bids.bestPrice()).isEqualTo(Long.MIN_VALUE);
        assertThat(bids.sharesAt(99_9900)).isZero();
    }

    @Test
    void ignoresAddAtAndAboveWindowTop() {
        BookSide asks = bookSide(Side.ASK);

        asks.add(110_2400, 500);
        asks.add(200_0000, 500);

        assertThat(asks.isEmpty()).isTrue();
        assertThat(asks.sharesAt(200_0000)).isZero();
    }

    @Test
    void outOfWindowRemoveIsNoOp() {
        BookSide bids = bookSide(Side.BID);
        bids.add(100_5000, 300);

        bids.remove(99_0000, 100);
        bids.remove(200_0000, 100);

        assertThat(bids.sharesAt(100_5000)).isEqualTo(300);
        assertThat(bids.bestPrice()).isEqualTo(100_5000);
        assertThat(bids.levelCount()).isEqualTo(1);
    }

    @Test
    void onlyInWindowOrdersCountWhenMixed() {
        BookSide bids = bookSide(Side.BID);

        bids.add(100_5000, 300);
        bids.add(99_0000, 999);
        bids.add(500_0000, 999);

        assertThat(bids.levelCount()).isEqualTo(1);
        assertThat(bids.bestPrice()).isEqualTo(100_5000);
        assertThat(bids.bestShares()).isEqualTo(300);
    }

    @Test
    void minInclusiveMaxExclusive() {
        BookSide asks = bookSide(Side.ASK);

        asks.add(100_0000, 10);
        asks.add(110_2400, 20);

        assertThat(asks.sharesAt(100_0000)).isEqualTo(10);
        assertThat(asks.sharesAt(110_2400)).isZero();
        assertThat(asks.levelCount()).isEqualTo(1);
    }
}
