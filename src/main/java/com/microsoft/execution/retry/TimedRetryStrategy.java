package com.microsoft.execution.retry;

public record TimedRetryStrategy(int maxRetries, long delayInMillis) implements RetryStrategy {

    public TimedRetryStrategy {
        if (maxRetries <= 0) {
            throw new IllegalArgumentException("maxRetries must be greater than 0");
        }

        if (delayInMillis < 0) {
            throw new IllegalArgumentException("delayInMillis must be greater than or equal to 0");
        }
    }

    @Override
    public boolean shouldRetry(int attempt) {
        return attempt < maxRetries;
    }
}
