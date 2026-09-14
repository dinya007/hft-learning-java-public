package com.tisov.denis.trader.strategy;

import com.tisov.denis.trader.book.OrderBook;
import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.sink.MdEventSink;

public class TradingPipeline implements MdEventSink {

    private final OrderBook orderBook;
    private final MdEventSink bookMdEventSink;
    private final Strategy strategy;

    public TradingPipeline(OrderBook orderBook, MdEventSink bookMdEventSink, Strategy strategy) {
        this.orderBook = orderBook;
        this.bookMdEventSink = bookMdEventSink;
        this.strategy = strategy;
    }

    @Override
    public void onEvent(MdEvent event) {
        bookMdEventSink.onEvent(event);
        strategy.onBookUpdate(orderBook, event.timestamp());
    }

}
