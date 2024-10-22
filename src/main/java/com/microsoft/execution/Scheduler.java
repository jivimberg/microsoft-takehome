package com.microsoft.execution;

import com.microsoft.model.DagNode;

import java.util.concurrent.BlockingQueue;

public class Scheduler {

    private final BlockingQueue<DagNode> nodesToBeExecuted;
    private final IDagNodeExecutor dagNodeExecutor;

    public Scheduler(BlockingQueue<DagNode> nodesToBeExecuted, IDagNodeExecutor dagNodeExecutor) {
        this.nodesToBeExecuted = nodesToBeExecuted;
        this.dagNodeExecutor = dagNodeExecutor;
    }

    public void start() {
        while (true) {
            try {
                // Decide which one to pick either based on the data structure used or something
                DagNode node = nodesToBeExecuted.take();
                dagNodeExecutor.executeAsync(node);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
