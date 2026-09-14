package com.tisov.denis.trader.book.side;

import com.tisov.denis.trader.domain.Side;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WindowedLadderBookSideTest extends BookSideTest {

    @Override
    BookSide bookSide(byte side) {
        return new WindowedLadderBookSide(side, 100_0000, 1_00, 1024, 16);
    }

    @Test
    void farAddIsRememberedButNotInBbo() {
        BookSide bids = bookSide(Side.BID);

        bids.add(200_0000, 500);

        assertThat(bids.sharesAt(200_0000)).isEqualTo(500);
        assertThat(bids.isEmpty()).isTrue();
        assertThat(bids.levelCount()).isZero();
        assertThat(bids.bestPrice()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void farLevelAggregatesThenDrainsAndIsForgotten() {
        BookSide asks = bookSide(Side.ASK);

        asks.add(50_0000, 300);
        asks.add(50_0000, 200);
        assertThat(asks.sharesAt(50_0000)).isEqualTo(500);

        asks.remove(50_0000, 200);
        assertThat(asks.sharesAt(50_0000)).isEqualTo(300);

        asks.remove(50_0000, 300);
        assertThat(asks.sharesAt(50_0000)).isZero();
    }

    @Test
    void overflowDoesNotAffectInWindowBook() {
        BookSide bids = bookSide(Side.BID);

        bids.add(105_0000, 100);
        bids.add(200_0000, 999);
        bids.add(50_0000, 999);

        assertThat(bids.bestPrice()).isEqualTo(105_0000);
        assertThat(bids.bestShares()).isEqualTo(100);
        assertThat(bids.levelCount()).isEqualTo(1);
        assertThat(bids.sharesAt(200_0000)).isEqualTo(999);
        assertThat(bids.sharesAt(50_0000)).isEqualTo(999);
    }

    @Test
    void farHigherBidNeverBecomesBest() {
        BookSide bids = bookSide(Side.BID);

        bids.add(105_0000, 10);
        bids.add(200_0000, 10);

        assertThat(bids.bestPrice()).isEqualTo(105_0000);
    }

    @Test
    void farLowerAskNeverBecomesBest() {
        BookSide asks = bookSide(Side.ASK);

        asks.add(105_0000, 10);
        asks.add(50_0000, 10);

        assertThat(asks.bestPrice()).isEqualTo(105_0000);
    }

    @Test
    void emptyWhenOnlyOverflowPresent() {
        BookSide bids = bookSide(Side.BID);

        bids.add(200_0000, 100);

        assertThat(bids.isEmpty()).isTrue();
        assertThat(bids.bestPrice()).isEqualTo(Long.MIN_VALUE);
        assertThat(bids.sharesAt(200_0000)).isEqualTo(100);
    }

}
