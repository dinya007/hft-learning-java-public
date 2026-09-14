package com.tisov.denis.e2e;

public record TickerConfig(
        String ticker,
        int locate,
        OrderBookConfig orderBookConfig,
        StrategyConfig strategyConfig
) {
}
