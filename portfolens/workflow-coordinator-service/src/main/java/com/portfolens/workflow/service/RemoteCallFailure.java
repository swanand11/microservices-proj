package com.portfolens.workflow.service;

public class RemoteCallFailure extends RuntimeException {
    private final boolean retryable;
    private final boolean outcomeUncertain;
    public RemoteCallFailure(String message, boolean retryable, boolean outcomeUncertain, Throwable cause) {
        super(message, cause); this.retryable = retryable; this.outcomeUncertain = outcomeUncertain;
    }
    public boolean retryable() { return retryable; }
    public boolean outcomeUncertain() { return outcomeUncertain; }
}
