package com.tisov.denis.trader.strategy;

import com.tisov.denis.trader.domain.OrderIntent;

@FunctionalInterface
public interface OrderIntentSink {

    void onIntent(OrderIntent orderIntent);

}
