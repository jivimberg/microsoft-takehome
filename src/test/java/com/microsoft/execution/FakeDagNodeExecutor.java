package com.microsoft.execution;

import com.microsoft.model.IDagNode;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class FakeDagNodeExecutor implements IDagNodeExecutor {

    private final Semaphore semaphore = new Semaphore(0);
    private final List<Integer> nodesExecuted = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService executorService;
    private final Set<Integer> nodesThatWillFail;

    public FakeDagNodeExecutor(int poolSize, Set<Integer> nodesThatWillFail) {
        this.nodesThatWillFail = nodesThatWillFail;
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public FakeDagNodeExecutor(int poolSize) {
        this(poolSize, new HashSet<>());
    }

    public void process(int amount) {
        semaphore.release(amount);
    }

    public int getThreadsWaiting() {
        return semaphore.getQueueLength();
    }

    public List<Integer> getNodesExecuted() {
        return nodesExecuted;
    }

    @Override
    public CompletableFuture<Integer> executeAsync(IDagNode unitOfExecution) {
        System.out.println("Node " + unitOfExecution.id() + " submitted for execution");

        return CompletableFuture.supplyAsync(() -> {
            semaphore.acquireUninterruptibly();

            System.out.println("Executing node " + unitOfExecution.id());
            System.out.println();

            nodesExecuted.add(unitOfExecution.id());

            if (nodesThatWillFail.contains(unitOfExecution.id())) {
                return -1;
            } else {
                return 0;
            }
        }, executorService);
    }
}
