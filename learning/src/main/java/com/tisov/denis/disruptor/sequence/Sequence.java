package com.tisov.denis.disruptor.sequence;

import jdk.internal.vm.annotation.Contended;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static com.tisov.denis.disruptor.sequencer.Sequencer.INITIAL_CURSOR_VALUE;

@Contended
public class Sequence {

    private long value;

    private static final VarHandle VALUE;

    static {
        try {
            VALUE = MethodHandles.lookup()
                    .findVarHandle(Sequence.class, "value", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public Sequence() {
        VALUE.setRelease(this, INITIAL_CURSOR_VALUE);
    }

    public long get() {
        return (long) VALUE.getAcquire(this);
    }

    public void set(long value) {
        VALUE.setRelease(this, value);
    }

    public boolean compareAndSet(long expected, long updated) {
        return VALUE.compareAndSet(this, expected, updated);
    }
}
