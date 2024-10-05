package com.microsoft.execution.retry;

public record ExponentialBackoffRetryStrategy(int maxRetries, long baseDelayInMillis, float multiplier) implements RetryStrategy {

    public ExponentialBackoffRetryStrategy {
        if (maxRetries <= 0) {
            throw new IllegalArgumentException("maxRetries must be greater than 0");
        }

        if (baseDelayInMillis <= 0) {
            throw new IllegalArgumentException("baseDelayInMillis must be greater than 0");
        }

        if (multiplier <= 1) {
            throw new IllegalArgumentException("multiplier must be greater than 1");
        }
    }

    @Override
    public boolean shouldRetry(int attempt) {
        return attempt < maxRetries;
    }

    public long getDelayInMillis(int attempt) {
        return (long) (baseDelayInMillis * Math.pow(multiplier, attempt));
    }
}
