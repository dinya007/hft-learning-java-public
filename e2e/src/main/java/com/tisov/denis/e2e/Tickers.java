package com.tisov.denis.e2e;

import java.util.Map;

public final class Tickers {

    public static final TickerConfig AMD = new TickerConfig(
            "AMD", 347,
            new OrderBookConfig(13_077, 2_502, 618, 310_000L, 100),
            new StrategyConfig(200, 100, 100));

    private static final Map<Integer, TickerConfig> BY_LOCATE = Map.of(AMD.locate(), AMD);

    private Tickers() {
    }

    public static TickerConfig byLocate(int locate) {
        TickerConfig c = BY_LOCATE.get(locate);
        if (c == null) {
            throw new IllegalArgumentException("no ticker config for locate " + locate);
        }
        return c;
    }
}
