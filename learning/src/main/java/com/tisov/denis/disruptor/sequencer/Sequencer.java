package com.tisov.denis.disruptor.sequencer;

import com.tisov.denis.disruptor.barrier.SequenceBarrier;
import com.tisov.denis.disruptor.sequence.Sequence;

public interface Sequencer {

    long INITIAL_CURSOR_VALUE = -1;

    long next();

    long next(int n);

    void publish(long sequence);

    void publish(long lo, long hi);

    boolean hasAvailableCapacity(int cap);

    long remainingCapacity();

    long getCursor();

    int getBufferSize();

    SequenceBarrier newBarrier(Sequence... sequence);

    void addGatingSequences(Sequence... gatingSequences);

    long getMinimumSequence();

    boolean isAvailable(long sequence);

}
