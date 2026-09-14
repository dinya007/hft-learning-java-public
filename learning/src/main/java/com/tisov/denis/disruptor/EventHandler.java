package com.tisov.denis.disruptor;

import com.tisov.denis.disruptor.sequence.Sequence;

public interface EventHandler {

    void onEvent(LongEvent event, long sequence, boolean endOfBatch);

    default void setSequenceCallback(Sequence sequence) {

    }

}
