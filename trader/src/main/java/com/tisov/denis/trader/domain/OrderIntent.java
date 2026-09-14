package com.tisov.denis.trader.domain;

public final class OrderIntent {

    private long timestamp;
    private long clientOrderId;
    private int symbolId;
    private byte side;
    private byte orderType;
    private long price;
    private int quantity;

    public OrderIntent asLimit(long timestamp, long clientOrderId, int symbolId, byte side, long price, int quantity) {
        this.timestamp = timestamp;
        this.clientOrderId = clientOrderId;
        this.symbolId = symbolId;
        this.side = side;
        this.orderType = OrderType.LIMIT;
        this.price = price;
        this.quantity = quantity;
        return this;
    }

    public long timestamp() {
        return timestamp;
    }

    public long clientOrderId() {
        return clientOrderId;
    }

    public int symbolId() {
        return symbolId;
    }

    public byte side() {
        return side;
    }

    public byte orderType() {
        return orderType;
    }

    public long price() {
        return price;
    }

    public int quantity() {
        return quantity;
    }

}
