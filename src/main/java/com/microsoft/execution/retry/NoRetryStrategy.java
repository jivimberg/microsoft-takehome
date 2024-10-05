package com.microsoft.execution.retry;

// NoRetryStrategy is a singleton
public enum NoRetryStrategy implements RetryStrategy {
    INSTANCE;

    @Override
    public boolean shouldRetry(int attempt) {
        return false;
    }
}
