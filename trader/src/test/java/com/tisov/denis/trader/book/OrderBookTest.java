package com.tisov.denis.trader.book;

import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import it.unimi.dsi.fastutil.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class OrderBookTest {

    private static final int CAPACITY = 64;

    public abstract Pair<OrderBook, MdEventSink> orderBook(int capacity);

    MdEvent ev = new MdEvent();
    OrderBook book;
    MdEventSink mdEventConsumer;

    @BeforeEach
    void setUp() {
        Pair<OrderBook, MdEventSink> bookAndMdEventSink = orderBook(CAPACITY);
        book = bookAndMdEventSink.first();
        mdEventConsumer = bookAndMdEventSink.second();
    }

    private void add(long ref, byte side, long price, long shares) {
        mdEventConsumer.onEvent(ev.asAdd(1L, 1, 0L, ref, side, shares, price));
    }

    private void execute(long ref, long shares) {
        mdEventConsumer.onEvent(ev.asExecute(1L, 1, ref, shares, 0L));
    }

    private void executeWithPrice(long ref, long shares, long printPrice) {
        mdEventConsumer.onEvent(ev.asExecuteWithPrice(1L, 1, ref, shares, 0L, printPrice));
    }

    private void cancel(long ref, long shares) {
        mdEventConsumer.onEvent(ev.asCancel(1L, 1, ref, shares));
    }

    private void delete(long ref) {
        mdEventConsumer.onEvent(ev.asDelete(1L, 1, ref));
    }

    private void replace(long oldRef, long newRef, long price, long shares) {
        mdEventConsumer.onEvent(ev.asReplace(1L, 1, oldRef, newRef, shares, price));
    }

    @Test
    void emptyBookHasNoBbo() {
        assertThat(book.bestBid()).isEqualTo(Long.MIN_VALUE);
        assertThat(book.bestAsk()).isEqualTo(Long.MIN_VALUE);
        assertThat(book.bestBidShares()).isZero();
        assertThat(book.bestAskShares()).isZero();
        assertThat(book.spread()).isEqualTo(Long.MIN_VALUE);
        assertThat(book.liveOrders()).isZero();
    }

    @Test
    void addBidSetsBestBid() {
        add(1, Side.BID, 100, 500);
        assertThat(book.bestBid()).isEqualTo(100);
        assertThat(book.bestBidShares()).isEqualTo(500);
        assertThat(book.liveOrders()).isEqualTo(1);
    }

    @Test
    void addBothSidesGivesBboAndSpread() {
        add(1, Side.BID, 100, 500);
        add(2, Side.ASK, 102, 300);
        assertThat(book.bestBid()).isEqualTo(100);
        assertThat(book.bestAsk()).isEqualTo(102);
        assertThat(book.bestAskShares()).isEqualTo(300);
        assertThat(book.spread()).isEqualTo(2);
        assertThat(book.liveOrders()).isEqualTo(2);
    }

    @Test
    void bestBidIsHighestAndBestAskIsLowest() {
        add(1, Side.BID, 100, 10);
        add(2, Side.BID, 102, 10);
        add(3, Side.BID, 101, 10);
        assertThat(book.bestBid()).isEqualTo(102);

        add(4, Side.ASK, 110, 10);
        add(5, Side.ASK, 108, 10);
        add(6, Side.ASK, 109, 10);
        assertThat(book.bestAsk()).isEqualTo(108);
    }

    @Test
    void aggregatesSharesAtSamePrice() {
        add(1, Side.BID, 100, 300);
        add(2, Side.BID, 100, 200);
        assertThat(book.bestBid()).isEqualTo(100);
        assertThat(book.bestBidShares()).isEqualTo(500);
        assertThat(book.liveOrders()).isEqualTo(2);
    }

    @Test
    void partialExecuteReducesLevelKeepsOrder() {
        add(1, Side.BID, 100, 500);
        execute(1, 200);
        assertThat(book.bestBid()).isEqualTo(100);
        assertThat(book.bestBidShares()).isEqualTo(300);
        assertThat(book.liveOrders()).isEqualTo(1);
    }

    @Test
    void fullExecuteRemovesOrderAndLevel() {
        add(1, Side.BID, 100, 500);
        execute(1, 500);
        assertThat(book.bestBid()).isEqualTo(Long.MIN_VALUE);
        assertThat(book.bestBidShares()).isZero();
        assertThat(book.liveOrders()).isZero();
    }

    @Test
    void executeWithPriceReducesAtOrderPriceNotPrintPrice() {
        add(1, Side.BID, 100, 500);
        executeWithPrice(1, 200, 99);
        assertThat(book.bestBid()).isEqualTo(100);
        assertThat(book.bestBidShares()).isEqualTo(300);
    }

    @Test
    void partialCancelReducesLevel() {
        add(1, Side.ASK, 100, 500);
        cancel(1, 200);
        assertThat(book.bestAsk()).isEqualTo(100);
        assertThat(book.bestAskShares()).isEqualTo(300);
        assertThat(book.liveOrders()).isEqualTo(1);
    }

    @Test
    void fullCancelRemovesOrder() {
        add(1, Side.ASK, 100, 500);
        cancel(1, 500);
        assertThat(book.bestAsk()).isEqualTo(Long.MIN_VALUE);
        assertThat(book.liveOrders()).isZero();
    }

    @Test
    void executeOnlyHitsReferencedOrderNotSameLevelSibling() {
        add(1, Side.BID, 100, 300);
        add(2, Side.BID, 100, 200);
        execute(1, 300);
        assertThat(book.bestBid()).isEqualTo(100);
        assertThat(book.bestBidShares()).isEqualTo(200);
        assertThat(book.liveOrders()).isEqualTo(1);
    }

    @Test
    void deleteRemovesEntireOrderAndMovesBest() {
        add(1, Side.BID, 100, 500);
        add(2, Side.BID, 99, 100);
        delete(1);
        assertThat(book.bestBid()).isEqualTo(99);
        assertThat(book.bestBidShares()).isEqualTo(100);
        assertThat(book.liveOrders()).isEqualTo(1);
    }

    @Test
    void deletePartiallyFilledOrderRemovesRemaining() {
        add(1, Side.BID, 100, 500);
        execute(1, 200);
        delete(1);
        assertThat(book.bestBid()).isEqualTo(Long.MIN_VALUE);
        assertThat(book.liveOrders()).isZero();
    }

    @Test
    void replaceMovesLevelAndRemapsRef() {
        add(1, Side.BID, 100, 500);
        replace(1, 2, 101, 400);
        assertThat(book.bestBid()).isEqualTo(101);
        assertThat(book.bestBidShares()).isEqualTo(400);
        assertThat(book.liveOrders()).isEqualTo(1);
    }

    @Test
    void replaceNewRefIsUsableForSubsequentOps() {
        add(1, Side.BID, 100, 500);
        replace(1, 2, 101, 400);
        execute(2, 400);
        assertThat(book.bestBid()).isEqualTo(Long.MIN_VALUE);
        assertThat(book.liveOrders()).isZero();
    }

    @Test
    void replaceInheritsSide() {
        add(1, Side.ASK, 110, 200);
        replace(1, 2, 108, 200);
        assertThat(book.bestAsk()).isEqualTo(108);
        assertThat(book.bestBid()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void liveOrdersTracksAddAndRemove() {
        add(1, Side.BID, 100, 100);
        add(2, Side.BID, 99, 100);
        add(3, Side.ASK, 101, 100);
        assertThat(book.liveOrders()).isEqualTo(3);
        delete(2);
        assertThat(book.liveOrders()).isEqualTo(2);
        execute(1, 100);
        assertThat(book.liveOrders()).isEqualTo(1);
        cancel(3, 100);
        assertThat(book.liveOrders()).isZero();
    }

    @Test
    void spreadIsMinValueWhenOneSideEmpty() {
        add(1, Side.BID, 100, 500);
        assertThat(book.spread()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void unknownRefFailsFast() {
        assertThatThrownBy(() -> delete(999))
                .isInstanceOfAny(AssertionError.class, IndexOutOfBoundsException.class);
    }

    @Test
    void overDrainFailsFast() {
        add(1, Side.BID, 100, 500);
        assertThatThrownBy(() -> execute(1, 600))
                .isInstanceOf(AssertionError.class);
    }

}
