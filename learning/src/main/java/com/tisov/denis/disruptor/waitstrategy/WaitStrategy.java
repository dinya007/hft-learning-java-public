package com.tisov.denis.disruptor.waitstrategy;

import com.tisov.denis.disruptor.barrier.SequenceBarrier;
import com.tisov.denis.disruptor.exception.AlertException;
import com.tisov.denis.disruptor.sequence.Sequence;

public interface WaitStrategy {

    long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier sequenceBarrier)
            throws AlertException, InterruptedException;

    void signalAllWhenBlocking();

}
