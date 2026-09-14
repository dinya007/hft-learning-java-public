package com.tisov.denis.disruptor.waitstrategy;

import com.tisov.denis.disruptor.barrier.SequenceBarrier;
import com.tisov.denis.disruptor.exception.AlertException;
import com.tisov.denis.disruptor.sequence.Sequence;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingWaitStrategy implements WaitStrategy {

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier sequenceBarrier) throws AlertException, InterruptedException {

        long availableSequence;
        if (cursor.get() < sequence) {

            lock.lock();
            try {
                while (cursor.get() < sequence) {
                    sequenceBarrier.checkAlert();
                    condition.await();
                }
            } finally {
                lock.unlock();
            }
        }

        while ((availableSequence = dependentSequence.get()) < sequence) {
            sequenceBarrier.checkAlert();
            Thread.onSpinWait();
        }

        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
