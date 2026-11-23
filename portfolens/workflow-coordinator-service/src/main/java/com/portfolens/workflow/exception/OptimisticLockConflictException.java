package com.portfolens.workflow.exception;

public class OptimisticLockConflictException extends RuntimeException {
    public OptimisticLockConflictException() { super("The workflow changed concurrently; reload and retry the operation"); }
}
