package com.tisov.denis.trader.signal;

import static com.tisov.denis.trader.signal.SignalSide.*;

public class OrderBookImbalanceSignal implements Signal {

    private final long p;
    private final long q;

    public OrderBookImbalanceSignal(long p, long q) {
        if (q <= 0) {
            throw new IllegalArgumentException("q must be > 0, got " + q);
        }
        if (p < 0 || p > q) {
            throw new IllegalArgumentException("p must be in [0, q], got p=" + p + " q=" + q);
        }

        this.p = p;
        this.q = q;
    }

    public byte get(long bidVolume, long askVolume) {
        long total = bidVolume + askVolume;
        long diff = bidVolume - askVolume;

        if (p * total < q * diff) {
            return UP;
        } else if (-p * total > q * diff) {
            return DOWN;
        } else {
            return FLAT;
        }
    }
}
