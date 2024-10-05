package com.microsoft.execution;

import com.microsoft.execution.retry.ExponentialBackoffRetryStrategy;
import com.microsoft.execution.retry.InfiteRetryStrategy;
import com.microsoft.execution.retry.TimedRetryStrategy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class RetryTest {

    @Test
    public void testMaxRetriesMustBeGreaterThanZeroForTimedRetryStrategy() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimedRetryStrategy(0, 1000);
        }, "Expected IllegalArgumentException for maxRetries <= 0");
    }

    @Test
    public void testDelayInMillisMustBeGreaterThanOrEqualToZeroForTimedRetryStrategy() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimedRetryStrategy(3, -1);
        }, "Expected IllegalArgumentException for delayInMillis < 0");
    }

    @Test
    public void testDelayInMillisMustBeGreaterThanOrEqualToZeroForInfiniteRetryStrategy() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InfiteRetryStrategy(-1);
        }, "Expected IllegalArgumentException for delayInMillis < 0");
    }

    @Test
    public void testMaxRetriesMustBeGreaterThanZeroForExponentialBackoffRetryStrategy() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ExponentialBackoffRetryStrategy(0, 1000, 2.0f);
        }, "Expected IllegalArgumentException for maxRetries <= 0");
    }

    @Test
    public void testBaseDelayInMillisMustBeGreaterThanZeroForExponentialBackoffRetryStrategy() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ExponentialBackoffRetryStrategy(3, -1, 2.0f);
        }, "Expected IllegalArgumentException for baseDelayInMillis <= 0");
    }

    @Test
    public void testMultiplierMustBeGreaterThanOneForExponentialBackoffRetryStrategy() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ExponentialBackoffRetryStrategy(3, 1000, 1.0f);
        }, "Expected IllegalArgumentException for multiplier <= 1");
    }

    @Test
    public void testTimedRetryStrategyNoDelay() throws InterruptedException, ExecutionException {
        TimedRetryStrategy retryStrategy = new TimedRetryStrategy(2, 0);
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 0.0f, retryStrategy);

        FailingDagNode node = new FailingDagNode(0, 2);

        CompletableFuture<Integer> future = dagNodeExecutor.executeAsync(node);
        int result = future.get();

        assertEquals(0, result, "Expected execution to succeed with result 0");
        assertEquals(3, node.getAttempts(), "Expected number of attempts differ");
    }

    @Test
    public void testExecutionFailsAfterRetries() throws InterruptedException, ExecutionException {
        TimedRetryStrategy retryStrategy = new TimedRetryStrategy(3, 0);
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 0.0f, retryStrategy);

        FailingDagNode node = new FailingDagNode(0, 5);

        CompletableFuture<Integer> future = dagNodeExecutor.executeAsync(node);
        int result = future.get();

        assertEquals(-1, result, "Expected execution to fail with result -1 after retries");
        assertEquals(4, node.getAttempts(), "Expected 4 attempts (initial + 3 retries)");
    }

    @Test
    public void testTimedRetries() throws InterruptedException, ExecutionException {
        TimedRetryStrategy retryStrategy = new TimedRetryStrategy(2, 500);
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 0.0f, retryStrategy);

        FailingDagNode node = new FailingDagNode(0, 2);

        long startTime = System.currentTimeMillis();
        CompletableFuture<Integer> future = dagNodeExecutor.executeAsync(node);

        await().until(() -> node.getAttempts() == 2); // Attempts is set to 2 when executing the first retry

        long firstAttemptTime = System.currentTimeMillis() - startTime;
        System.out.println("First attempt time: " + firstAttemptTime);
        assertTrue(firstAttemptTime > 500);

        await().until(() -> node.getAttempts() == 3); // Attempts is set to 3 when executing the second retry

        long secondAttemptTime = System.currentTimeMillis() - startTime;
        System.out.println("Second attempt time: " + secondAttemptTime);
        assertTrue(secondAttemptTime > 1000);

        int result = future.get();

        assertEquals(0, result, "Expected execution to succeed with result 0");
    }

    @Test
    public void testExponentialBackoffRetries() throws InterruptedException, ExecutionException {
        ExponentialBackoffRetryStrategy retryStrategy = new ExponentialBackoffRetryStrategy(2, 500, 2);
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 0.0f, retryStrategy);

        FailingDagNode node = new FailingDagNode(0, 2);

        long startTime = System.currentTimeMillis();
        CompletableFuture<Integer> future = dagNodeExecutor.executeAsync(node);

        await().until(() -> node.getAttempts() == 2);

        long firstAttemptTime = System.currentTimeMillis() - startTime;
        System.out.println("First attempt time: " + firstAttemptTime);
        assertTrue(firstAttemptTime > 500);

        await().until(() -> node.getAttempts() == 3);

        long secondAttemptTime = System.currentTimeMillis() - startTime;
        System.out.println("Second attempt time: " + secondAttemptTime);
        assertTrue(secondAttemptTime > 1500); // 500ms + 1000ms (exponential backoff)

        int result = future.get();

        assertEquals(0, result, "Expected execution to succeed with result 0");
    }

}
