package com.microsoft.execution.retry;

public sealed interface RetryStrategy permits ExponentialBackoffRetryStrategy, InfiteRetryStrategy, NoRetryStrategy, TimedRetryStrategy {
    boolean shouldRetry(int attempt);
}