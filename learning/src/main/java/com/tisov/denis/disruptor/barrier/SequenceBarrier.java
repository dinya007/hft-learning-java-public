package com.tisov.denis.disruptor.barrier;

import com.tisov.denis.disruptor.exception.AlertException;

public interface SequenceBarrier {

    long waitFor(long sequence) throws AlertException, InterruptedException;

    long getCursor();

    boolean isAlerted();

    void alert();

    void clearAlert();

    void checkAlert() throws AlertException;

}
