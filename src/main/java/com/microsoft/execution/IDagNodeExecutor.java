package com.microsoft.execution;

import com.microsoft.model.IDagNode;

import java.util.concurrent.CompletableFuture;

/**
 * Execution engine responsible to execute the nodes in the graph.
 */
public interface IDagNodeExecutor {
    /**
     * Executes a node in the graph.
     *
     * @param unitOfExecution The node to be executed.
     * @return 0 if success, < 0 otherwise.
     */
    CompletableFuture<Integer> executeAsync(IDagNode unitOfExecution);
}
