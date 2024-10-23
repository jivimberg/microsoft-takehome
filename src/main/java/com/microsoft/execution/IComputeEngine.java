package com.microsoft.execution;

import com.microsoft.model.DagNode;
import com.microsoft.model.IDagNode;

import java.util.concurrent.CompletableFuture;

/**
 * Takes care of executing the IWork tasks provided.
 * I can accept more work until resources are fully utilized.
 * It can run multiple tasks in parallel.
 */
public interface IComputeEngine {

    // Update available compute Power.
    public CompletableFuture<Integer> executeAsync(IWork dagNode);

    public Integer computePower();

    public void updateComputePower(Integer consumedPower);

    public Integer availableComputePower();
}
