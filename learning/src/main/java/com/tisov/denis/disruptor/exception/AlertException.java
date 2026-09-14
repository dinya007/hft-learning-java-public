package com.tisov.denis.disruptor.exception;

public class AlertException extends Exception {

    public static final AlertException INSTANCE = new AlertException();

    private AlertException() {
        super(null, null, true, false);
    }
}
