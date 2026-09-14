package com.tisov.denis.disruptor.sequencer;

import com.tisov.denis.disruptor.waitstrategy.WaitStrategy;
import jdk.internal.vm.annotation.Contended;

@Contended
public class SingleProducerSequencer extends AbstractSequencer {

    public SingleProducerSequencer(int bufferSize, WaitStrategy waitStrategy) {
        super(bufferSize, waitStrategy);
    }

    @Override
    public long next() {

        return 0;
    }

    @Override
    public long next(int n) {
        return 0;
    }

    @Override
    public void publish(long sequence) {
        cursor.set(sequence);
        waitStrategy.signalAllWhenBlocking();
    }

    @Override
    public void publish(long lo, long hi) {

    }

    @Override
    public boolean hasAvailableCapacity(int cap) {
        return false;
    }

    @Override
    public long remainingCapacity() {
        return 0;
    }

    @Override
    public boolean isAvailable(long sequence) {
        return false;
    }
}
