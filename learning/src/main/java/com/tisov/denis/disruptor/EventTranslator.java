package com.tisov.denis.disruptor;

public interface EventTranslator<T> {

    void translateTo(T event, long sequence);

}
