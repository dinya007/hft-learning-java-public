package com.tisov.denis.e2e;

public record OrderBookConfig(
        int maxOrders,
        int priceLevels,
        int priceFarLevels,
        long minTrackingPrice,
        long tickSize
) {
    public int poolCapacity() {
        return maxOrders <= 1 ? 1 : Integer.highestOneBit(maxOrders - 1) << 1;
    }

    public int overflowCapacity() {
        return priceFarLevels <= 1 ? 1 : Integer.highestOneBit(priceFarLevels - 1) << 1;
    }
}
