package com.tisov.denis.trader.id;

public class OrderIdGenerator {

    private static long MARKET_MAKER_STRATEGY_ID_GENERATOR = 0;

    public static long getNextMarketMakerStrategyId() {
        return MARKET_MAKER_STRATEGY_ID_GENERATOR++;
    }

    public static void reset() {
        MARKET_MAKER_STRATEGY_ID_GENERATOR = 0L;
    }

}
