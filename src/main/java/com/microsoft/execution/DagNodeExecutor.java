package com.microsoft.execution;

import com.microsoft.execution.retry.*;
import com.microsoft.model.IDagNode;
import com.microsoft.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class DagNodeExecutor implements IDagNodeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DagNodeExecutor.class);

    private final ScheduledExecutorService executorService;
    private final float failureRate;
    private final RetryStrategy retryStrategy;
    private final ResourceManager resourceManager;

    public DagNodeExecutor(int numberOfEngines, float failureRate, RetryStrategy retryStrategy) {
        if (failureRate < 0 || failureRate > 1) {
            throw new IllegalArgumentException("Failure rate must be between 0 and 1");
        }
        this.executorService = Executors.newScheduledThreadPool(numberOfEngines);
        this.failureRate = failureRate;
        this.retryStrategy = retryStrategy;
        this.resourceManager = new ResourceManager();
    }

    @Override
    public CompletableFuture<Integer> executeAsync(IDagNode unitOfExecution) {
        return executeWithRetry(unitOfExecution, 0);
    }

    private CompletableFuture<Integer> executeWithRetry(IDagNode unitOfExecution, int attempt) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        if (!resourceManager.acquireResources(unitOfExecution.resources())) {
                            throw new ResourcesNotAvailableException("Resources not available for node: " + unitOfExecution.id());
                        }

                        if (Math.random() < failureRate) {
                            throw new RuntimeException("Simulated failure for node: " + unitOfExecution.id());
                        }

                        unitOfExecution.execute();

                        return 0; // success
                    } finally {
                        resourceManager.releaseResources(unitOfExecution.resources());
                    }
                }, executorService)
                .exceptionallyCompose(ex -> {
                    if (ex instanceof ResourcesNotAvailableException) {
                        // Always retry if the resources were not available
                        logger.info("Resources not available for node: {}. Retrying after 100ms", unitOfExecution.id());
                        return scheduleRetry(unitOfExecution, attempt + 1, 100);
                    }

                    //noinspection StringConcatenationArgumentToLogCall
                    logger.error("Error executing node: " + unitOfExecution.id(), ex);

                    if (retryStrategy.shouldRetry(attempt)) {
                        logger.info("Retrying node: {}. Attempt number: {}", unitOfExecution.id(), attempt);

                        long delayInMillis = switch (retryStrategy) {
                            case TimedRetryStrategy timed -> timed.delayInMillis();
                            case InfiteRetryStrategy infinite -> infinite.delayInMillis();
                            case ExponentialBackoffRetryStrategy exponential -> exponential.getDelayInMillis(attempt);
                            case NoRetryStrategy _ -> throw new IllegalStateException("Can't retry with NoRetryStrategy");
                        };

                        return scheduleRetry(unitOfExecution, attempt + 1, delayInMillis);
                    } else {
                        logger.error("Retries exhausted for node: {}", unitOfExecution.id());
                        return CompletableFuture.completedFuture(-1);
                    }
                });
    }

    private CompletableFuture<Integer> scheduleRetry(
            IDagNode unitOfExecution,
            int retriesRemaining,
            long delayInMillis
    ) {
        // This conversion of Runnable to CompletableFuture is required because the executor returns a ScheduledFuture instead of CompletableFuture.
        CompletableFuture<Integer> retryFuture = new CompletableFuture<>();
        Runnable retry = () -> executeWithRetry(unitOfExecution, retriesRemaining).whenComplete((result, ex) -> {
            if (ex != null) {
                retryFuture.completeExceptionally(ex);
            } else {
                retryFuture.complete(result);
            }
        });

        executorService.schedule(retry, delayInMillis, TimeUnit.MILLISECONDS);

        return retryFuture;
    }
}
