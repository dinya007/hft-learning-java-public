package com.tisov.denis.trader.sink;

import com.tisov.denis.trader.domain.MdEvent;

@FunctionalInterface
public interface MdEventSink {
    void onEvent(MdEvent event);
}
