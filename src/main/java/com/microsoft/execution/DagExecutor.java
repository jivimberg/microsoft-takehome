package com.microsoft.execution;

import com.microsoft.model.ExecutionDag;
import com.microsoft.parser.IDagParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DagExecutor implements IDagExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DagExecutor.class);

    private final IDagParser dagParser;
    private final IDagNodeExecutor dagNodeExecutor;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public DagExecutor(IDagParser dagParser, IDagNodeExecutor dagNodeExecutor) {
        this.dagParser = dagParser;
        this.dagNodeExecutor = dagNodeExecutor;
    }

    @Override
    public CompletableFuture<DagResponse> processRequestAsync(DagRequest request) {
        // Parses request payload to a DAG Object
        ExecutionDag dag = dagParser.parseDag(request.dagXml());

        // Submits the DAG for execution
        return CompletableFuture.supplyAsync(() -> { // TODO check thread pools
            try {
                return executeDag(dag);
            } catch (InterruptedException ex) {
                logger.error("Dag execution failed", ex);
                return new DagResponse(true);
            }
        }, executorService);
    }

    private DagResponse executeDag(ExecutionDag dag) throws InterruptedException {
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();
        Map<Integer, Integer> inDegree = dag.getInDegree();
        int dagSize = adjacencyList.size();

        // Queue to store nodes that are ready to be executed
        BlockingQueue<Integer> q = new LinkedBlockingQueue<>(); // using max capacity to avoid blocking on add
        for (int i = 0; i < inDegree.size(); i++) {
            if (inDegree.get(i) == 0) {
                q.put(i);
            }
        }

        ConcurrentHashMap<Integer, Integer> concurrentInDegree = new ConcurrentHashMap<>(inDegree);
        AtomicBoolean hasFailed = new AtomicBoolean(false);
        Semaphore semaphore = new Semaphore(0);
        int nodesScheduledForExecution = 0;

        while (nodesScheduledForExecution < dagSize) {
            logger.info("Blocking execution for DAG " + dag.hashCode());
            int node = q.take();
            logger.info("Taking item for DAG " + dag.hashCode());

            if(node < 0) {
                // Poison pill received, stop executing
                break;
            }

            // Execute the node
            dagNodeExecutor.executeAsync(dag.getNode(node))
                    .thenAccept(result -> {
                        if (result < 0) {
                            throw new RuntimeException("Node execution failed");
                        }

                        semaphore.release();

                        // Decrease inDegree of neighbors
                        for (int nodeId : adjacencyList.get(node)) {
                            int newInDegree = concurrentInDegree.merge(nodeId, -1, Integer::sum);

                            if (newInDegree == 0) { // If inDegree becomes 0, push it to the queue
                                q.add(nodeId);
                            } else if (newInDegree < 0) { // Should never happen if the DAG is correct
                                throw new IllegalStateException("Negative inDegree detected");
                            }
                        }
                    })
                    .exceptionally(
                            ex -> {
                                logger.error("Node execution failed", ex);
                                hasFailed.set(true);
                                semaphore.release(dagSize); // Release all the permits to unblock the DAG execution.
                                q.add(-1); // send poison pill to unblock thread waiting on the queue
                                return null;
                            }
                    );

            nodesScheduledForExecution++;
        }

        semaphore.acquire(dagSize); // Wait for all executions to complete or for one to fail.

        return new DagResponse(hasFailed.get());
    }
}
