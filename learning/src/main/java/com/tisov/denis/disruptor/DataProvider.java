package com.tisov.denis.disruptor;

public interface DataProvider<T> {

    T get(long sequence);

}
