package com.microsoft.execution.retry;

public record InfiteRetryStrategy(long delayInMillis) implements RetryStrategy {

    public InfiteRetryStrategy {
        if (delayInMillis < 0) {
            throw new IllegalArgumentException("delayInMillis must be greater or equal to 0");
        }
    }

    @Override
    public boolean shouldRetry(int attempt) {
        return true;
    }
}
