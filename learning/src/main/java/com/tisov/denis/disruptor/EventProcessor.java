package com.tisov.denis.disruptor;

import com.tisov.denis.disruptor.sequence.Sequence;

public interface EventProcessor<T> extends Runnable {

    Sequence getSequence();

    void halt();

    boolean isRunning();

}
