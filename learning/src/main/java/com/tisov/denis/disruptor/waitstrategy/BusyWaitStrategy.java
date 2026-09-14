package com.tisov.denis.disruptor.waitstrategy;

import com.tisov.denis.disruptor.barrier.SequenceBarrier;
import com.tisov.denis.disruptor.exception.AlertException;
import com.tisov.denis.disruptor.sequence.Sequence;

public class BusyWaitStrategy implements WaitStrategy {

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier sequenceBarrier) throws AlertException, InterruptedException {
        long availableSequence;
        while ((availableSequence = dependentSequence.get()) < sequence) {
            sequenceBarrier.checkAlert();
        }
        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking() {
    }
}
