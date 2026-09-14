package com.tisov.denis.trader.domain;

public final class MdEvent {

    public static final byte TYPE_ADD = 1;
    public static final byte TYPE_EXECUTE = 2;
    public static final byte TYPE_EXECUTE_PRICE = 3;
    public static final byte TYPE_CANCEL = 4;
    public static final byte TYPE_DELETE = 5;
    public static final byte TYPE_REPLACE = 6;

    private byte type;
    private long timestamp;
    private int locate;

    private long orderRef;

    private long newOrderRef;

    private long stock;
    private byte side;

    private long shares;

    private long price;

    private long matchNumber;

    public MdEvent asAdd(long timestamp, int locate, long stock, long orderRef, byte side, long shares, long price) {
        this.type = TYPE_ADD;
        this.timestamp = timestamp;
        this.locate = locate;
        this.stock = stock;
        this.orderRef = orderRef;
        this.side = side;
        this.shares = shares;
        this.price = price;
        return this;
    }

    public MdEvent asExecute(long timestamp, int locate, long orderRef, long executedShares, long matchNumber) {
        this.type = TYPE_EXECUTE;
        this.timestamp = timestamp;
        this.locate = locate;
        this.orderRef = orderRef;
        this.shares = executedShares;
        this.matchNumber = matchNumber;
        return this;
    }

    public MdEvent asExecuteWithPrice(long timestamp, int locate, long orderRef, long executedShares, long matchNumber, long price) {
        this.type = TYPE_EXECUTE_PRICE;
        this.timestamp = timestamp;
        this.locate = locate;
        this.orderRef = orderRef;
        this.shares = executedShares;
        this.matchNumber = matchNumber;
        this.price = price;
        return this;
    }

    public MdEvent asCancel(long timestamp, int locate, long orderRef, long cancelledShares) {
        this.type = TYPE_CANCEL;
        this.timestamp = timestamp;
        this.locate = locate;
        this.orderRef = orderRef;
        this.shares = cancelledShares;
        return this;
    }

    public MdEvent asDelete(long ts, int locate, long orderRef) {
        this.type = TYPE_DELETE;
        this.timestamp = ts;
        this.locate = locate;
        this.orderRef = orderRef;
        return this;
    }

    public MdEvent asReplace(long timestamp, int locate, long origOrderRef, long newOrderRef,
                             long shares, long price) {
        this.type = TYPE_REPLACE;
        this.timestamp = timestamp;
        this.locate = locate;
        this.orderRef = origOrderRef;
        this.newOrderRef = newOrderRef;
        this.shares = shares;
        this.price = price;
        return this;
    }

    public byte type() {
        return type;
    }

    public long timestamp() {
        return timestamp;
    }

    public int locate() {
        return locate;
    }

    public long orderRef() {
        return orderRef;
    }

    public long newOrderRef() {
        return newOrderRef;
    }

    public long stock() {
        return stock;
    }

    public byte side() {
        return side;
    }

    public long shares() {
        return shares;
    }

    public long price() {
        return price;
    }

    public long matchNumber() {
        return matchNumber;
    }

}
