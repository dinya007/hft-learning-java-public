package com.tisov.denis.lock;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class Mutex {

    private final AtomicReference<Thread> owner = new AtomicReference<>(null);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();

    private static final int MAX_SPIN_WAIT = 64;

    public void lock() {
        Thread me = Thread.currentThread();

        if (owner.get() == me) {
            throw new IllegalMonitorStateException("Mutex is non-reentrant");
        }

        boolean wasInterrupted = false;

        while (!spinWait(me)) {
            waiters.add(me);

            if (owner.compareAndSet(null, me)) {
                waiters.remove(me);
                break;
            }

            LockSupport.park(this);

            waiters.remove(me);

            if (Thread.interrupted()) {
                wasInterrupted = true;
            }
        }

        if (wasInterrupted) {
            me.interrupt();
        }
    }

    private boolean spinWait(Thread me) {
        for (int i = 0; i < MAX_SPIN_WAIT; i++) {
            if (owner.compareAndSet(null, me)) {
                return true;
            }
            Thread.onSpinWait();
        }
        return false;
    }

    public void unlock() {

        owner.setRelease(null);

        Thread next = waiters.poll();
        if (next != null) {
            LockSupport.unpark(next);
        }
    }

}
