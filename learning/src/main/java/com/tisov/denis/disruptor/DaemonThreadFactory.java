package com.tisov.denis.disruptor;

import java.util.concurrent.ThreadFactory;

public enum DaemonThreadFactory implements ThreadFactory {
    INSTANCE;

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread();
        thread.setDaemon(true);
        return thread;
    }
}
