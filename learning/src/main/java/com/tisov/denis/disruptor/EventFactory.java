package com.tisov.denis.disruptor;

public interface EventFactory<T> {

    T newInstance();

}
