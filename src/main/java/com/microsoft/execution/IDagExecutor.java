package com.microsoft.execution;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for the execution of a DAG.
 */
public interface IDagExecutor {
    /**
     * Processes a Dag request and returns a response: success or failure.
     *
     * @param request The request to be processed
     * @return The response.
     */
    CompletableFuture<DagResponse> processRequestAsync(DagRequest request);
}
