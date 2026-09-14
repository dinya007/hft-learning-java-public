package com.tisov.denis.disruptor.waitstrategy;

import com.tisov.denis.disruptor.barrier.SequenceBarrier;
import com.tisov.denis.disruptor.exception.AlertException;
import com.tisov.denis.disruptor.sequence.Sequence;

public class YieldingWaitStrategy implements WaitStrategy {

    private static final int SPIN_TRIES = 100;

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier sequenceBarrier)
            throws AlertException, InterruptedException {
        long availableSequence;
        int tryCount = SPIN_TRIES;

        while ((availableSequence = dependentSequence.get()) < sequence) {
            sequenceBarrier.checkAlert();

            if (tryCount == 0) {
                Thread.yield();
            } else {
                Thread.onSpinWait();
                --tryCount;
            }
        }

        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking() {

    }
}
