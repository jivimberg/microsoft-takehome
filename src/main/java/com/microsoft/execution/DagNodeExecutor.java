package com.microsoft.execution;

import com.microsoft.model.IDagNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DagNodeExecutor implements IDagNodeExecutor {

    private final ExecutorService executorService;
    private final float failureRate;

    public DagNodeExecutor(int numberOfEngines, float failureRate) {
        if (failureRate < 0 || failureRate > 1) {
            throw new IllegalArgumentException("Failure rate must be between 0 and 1");
        }
        this.executorService = Executors.newFixedThreadPool(numberOfEngines); // TODO introduce retry policy by using scheduled thread pool
        this.failureRate = failureRate;
    }

    @Override
    public CompletableFuture<Integer> executeAsync(IDagNode unitOfExecution) {
        return CompletableFuture.supplyAsync(() -> {
            if (Math.random() < failureRate) {
                return -1;
            }

            try {
                unitOfExecution.execute();
                return 0;
            } catch (Exception e) {
                return -1;
            }
        }, executorService);
    }
}
