package com.tisov.denis.memoryorder;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class Counter {

    private long counter;

    private static final VarHandle COUNTER;

    static {
        try {
            COUNTER = MethodHandles.lookup().findVarHandle(Counter.class, "counter", long.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public long get() {
        return counter;
    }

    public long incrementPlain() {
        return ++counter;
    }

    public long incrementPlainVar() {
        long c = (long) COUNTER.get(this);
        c += 1;
        COUNTER.set(this, c);
        return c;
    }

    public long incrementRelaxed() {
        long c = (long) COUNTER.getOpaque(this);
        c += 1;
        COUNTER.setOpaque(this, c);
        return c;
    }

    public long incrementAcquireRelease() {
        long c = (long) COUNTER.getAcquire(this);
        c += 1;
        COUNTER.setRelease(this, c);
        return c;
    }

    public long incrementVolatile() {
        long c = (long) COUNTER.getVolatile(this);
        c += 1;
        COUNTER.setVolatile(this, c);
        return c;
    }

    public long incrementCAS() {
        long current, next;
        do {
            current = (long) COUNTER.getVolatile(this);
            next = current + 1;
        } while (!COUNTER.compareAndSet(this, current, next));
        return next;
    }

    public long incrementGetAndAdd() {
        return (long) COUNTER.getAndAdd(this, 1L);
    }
}
