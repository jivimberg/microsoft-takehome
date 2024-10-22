package com.microsoft.execution;

import com.microsoft.execution.retry.*;
import com.microsoft.model.IDagNode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class DagNodeExecutor implements IDagNodeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DagNodeExecutor.class);

    private final float failureRate;
    private final RetryStrategy retryStrategy;
    private final ThreadPoolExecutor executorService;

    public DagNodeExecutor(int numberOfEngines, float failureRate, RetryStrategy retryStrategy) {
        if (failureRate < 0 || failureRate > 1) {
            throw new IllegalArgumentException("Failure rate must be between 0 and 1");
        }
        PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>(
                11,
                (task1, task2) -> {
                    if (task1 instanceof PriorityRunnable && task2 instanceof PriorityRunnable) {
                        return ((PriorityRunnable) task1).compareTo((PriorityRunnable) task2);
                    }
                    return 0;
                }
        );
        this.executorService = new ThreadPoolExecutor(numberOfEngines, numberOfEngines, 0L, TimeUnit.MILLISECONDS, queue);
        this.failureRate = failureRate;
        this.retryStrategy = retryStrategy;
    }

    @Override
    public CompletableFuture<Integer> executeAsync(IDagNode unitOfExecution) {
        return executeWithRetry(unitOfExecution, 0, 0);
    }

    private CompletableFuture<Integer> executeWithRetry(IDagNode unitOfExecution, int attempt, long delayInMillis) {
        PriorityRunnable task = new PriorityRunnable(unitOfExecution, failureRate, delayInMillis, TimeUnit.MILLISECONDS);
        return CompletableFuture
                .runAsync(task, executorService)
                .handle((_, ex) -> {
                    if (ex != null) {
                        //noinspection StringConcatenationArgumentToLogCall
                        logger.error("Error executing node: " + unitOfExecution.id(), ex);

                        if (retryStrategy.shouldRetry(attempt)) {
                            logger.info("Retrying node: {}. Attempt number: {}", unitOfExecution.id(), attempt);

                            long retryDelayInMillis = switch (retryStrategy) {
                                case TimedRetryStrategy timed -> timed.delayInMillis();
                                case InfiteRetryStrategy infinite -> infinite.delayInMillis();
                                case ExponentialBackoffRetryStrategy exponential -> exponential.getDelayInMillis(attempt);
                                case NoRetryStrategy _ -> throw new IllegalStateException("Can't retry with NoRetryStrategy");
                            };

                            return executeWithRetry(unitOfExecution, attempt + 1, retryDelayInMillis).join();
                        } else {
                            logger.error("Retries exhausted for node: {}", unitOfExecution.id());
                            return -1;
                        }
                    } else {
                        return 0;
                    }
                });
    }

    private static final class PriorityRunnable implements Runnable, Delayed {
        private final IDagNode node;
        private final float failureRate;
        private final long scheduledTime;

        private PriorityRunnable(IDagNode node, float failureRate, long delay, TimeUnit timeUnit) {
            this.node = node;
            this.failureRate = failureRate;
            this.scheduledTime = System.currentTimeMillis() + timeUnit.toMillis(delay);
        }

        @Override
        public void run() {
            if (Math.random() < this.failureRate) {
                throw new RuntimeException("Simulated failure for node: " + node.id());
            }

            node.execute();
        }

        @Override
        public int compareTo(@NotNull Delayed other) {
            if (other instanceof PriorityRunnable otherTask) {
                // Compare by priority first (lower value = higher priority)
                int priorityComparison = Integer.compare(this.node.priority(), otherTask.node.priority());
                if (priorityComparison != 0) {
                    return priorityComparison;
                }

                // If priority is the same, compare by delay
                return Long.compare(this.scheduledTime, otherTask.scheduledTime);
            }

            return 0;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return unit.convert(scheduledTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (PriorityRunnable) obj;
            return Objects.equals(this.node, that.node) &&
                    Float.floatToIntBits(this.failureRate) == Float.floatToIntBits(that.failureRate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, failureRate);
        }

        @Override
        public String toString() {
            return "PriorityRunnable[" +
                    "node=" + node + ", " +
                    "failureRate=" + failureRate + ']';
        }

    }
}
