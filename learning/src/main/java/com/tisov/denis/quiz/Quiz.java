package com.tisov.denis.quiz;

public interface Quiz {

    default String name() {
        return getClass().getSimpleName();
    }

    String topic();

    void run();
}
