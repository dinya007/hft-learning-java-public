package com.tisov.denis.trader.signal;

public interface Signal {

    byte get(long bidVolume, long askVolume);

}
