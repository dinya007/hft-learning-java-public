package com.tisov.denis.disruptor.sequencer;

import com.tisov.denis.disruptor.barrier.ProcessingSequenceBarrier;
import com.tisov.denis.disruptor.barrier.SequenceBarrier;
import com.tisov.denis.disruptor.sequence.FixedSequenceGroup;
import com.tisov.denis.disruptor.sequence.Sequence;
import com.tisov.denis.disruptor.utils.SequenceUtils;
import com.tisov.denis.disruptor.waitstrategy.WaitStrategy;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

public abstract class AbstractSequencer implements Sequencer {

    protected final int bufferSize;
    protected final WaitStrategy waitStrategy;
    protected final Sequence cursor = new Sequence();
    private Sequence[] gatingSequences = new Sequence[0];
    private static final VarHandle GATING_SEQUENCES;

    static {
        try {
            GATING_SEQUENCES = MethodHandles.lookup()
                    .findVarHandle(AbstractSequencer.class, "gatingSequences", Sequence[].class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public AbstractSequencer(int bufferSize, WaitStrategy waitStrategy) {
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("bufferSize must be power of 2, was: %d".formatted(bufferSize));
        }
        this.bufferSize = bufferSize;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public long getCursor() {
        return cursor.get();
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void addGatingSequences(Sequence... gatingSequences) {
        Sequence[] current, updated;
        do {
            current = (Sequence[]) GATING_SEQUENCES.getAcquire(this);
            updated = Arrays.copyOf(current, current.length + gatingSequences.length);
            System.arraycopy(gatingSequences, 0, updated, current.length, gatingSequences.length);

        } while (!GATING_SEQUENCES.compareAndSet(this, current, updated));
    }

    @Override
    public long getMinimumSequence() {
        return SequenceUtils.min((Sequence[]) GATING_SEQUENCES.getAcquire(this), cursor.get());
    }

    @Override
    public SequenceBarrier newBarrier(Sequence... sequences) {
        Sequence dependent = sequences.length != 0 ? new FixedSequenceGroup(sequences) : cursor;
        return new ProcessingSequenceBarrier(cursor, dependent, waitStrategy);
    }
}
