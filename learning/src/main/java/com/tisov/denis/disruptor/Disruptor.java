package com.tisov.denis.disruptor;

import com.tisov.denis.disruptor.processor.BatchEventProcessor;
import com.tisov.denis.disruptor.waitstrategy.WaitStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

public final class Disruptor<T> {

    private final RingBuffer<T> ringBuffer;
    private final ThreadFactory threadFactory;
    private final List<BatchEventProcessor<T>> processors = new ArrayList<>();

    public Disruptor(EventFactory<T> eventFactory, int bufferSize, ThreadFactory threadFactory,
                     ProducerType producerType, WaitStrategy waitStrategy) {
        this.ringBuffer = null;
        this.threadFactory = threadFactory;
    }

}
