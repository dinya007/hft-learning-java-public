package com.tisov.denis.disruptor.sequence;

import com.tisov.denis.disruptor.utils.SequenceUtils;

public class FixedSequenceGroup extends Sequence {

    private final Sequence[] dependentSequences;

    public FixedSequenceGroup(Sequence[] dependentSequences) {
        this.dependentSequences = dependentSequences;
    }

    @Override
    public long get() {
        return SequenceUtils.min(dependentSequences);
    }

    @Override
    public void set(long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSet(long expected, long updated) {
        throw new UnsupportedOperationException();
    }
}
