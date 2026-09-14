package com.tisov.denis.disruptor.barrier;

import com.tisov.denis.disruptor.exception.AlertException;
import com.tisov.denis.disruptor.sequence.Sequence;
import com.tisov.denis.disruptor.waitstrategy.WaitStrategy;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class ProcessingSequenceBarrier implements SequenceBarrier {

    private final Sequence cursor;
    private final Sequence dependentSequence;
    private final WaitStrategy waitStrategy;

    private boolean alerted;
    private static final VarHandle ALERTED;

    static {
        try {
            ALERTED = MethodHandles.lookup().findVarHandle(ProcessingSequenceBarrier.class, "alerted", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public ProcessingSequenceBarrier(Sequence cursor, Sequence dependentSequence, WaitStrategy waitStrategy) {
        this.cursor = cursor;
        this.dependentSequence = dependentSequence;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public long waitFor(long sequence) throws AlertException, InterruptedException {
        checkAlert();
        return waitStrategy.waitFor(sequence, cursor, dependentSequence, this);
    }

    @Override
    public long getCursor() {
        return cursor.get();
    }

    @Override
    public boolean isAlerted() {
        return (boolean) ALERTED.getAcquire(this);
    }

    @Override
    public void alert() {
        ALERTED.setRelease(this, true);
        waitStrategy.signalAllWhenBlocking();
    }

    @Override
    public void clearAlert() {
        ALERTED.setRelease(this, false);
    }

    @Override
    public void checkAlert() throws AlertException {
        if ((boolean) ALERTED.getAcquire(this)) {
            throw AlertException.INSTANCE;
        }
    }
}
