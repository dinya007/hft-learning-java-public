package com.tisov.denis.e2e;

import com.tisov.denis.trader.book.OrderBook;
import com.tisov.denis.trader.sink.MdEventSink;

public record Pipeline(MdEventSink sink, OrderBook book) {
}
