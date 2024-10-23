package com.microsoft.execution;

import com.microsoft.model.DagNode;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decides which IWork will run in which IComputeEngine, based on some policy
 */
public class Scheduler {

    private final List<DagNode> nodesToBeExecuted; // Thing will accumulate here for execution
    private final IDagNodeExecutor dagNodeExecutor;
    private final PriorityQueue<IComputeEngine> computeEngines;
    private final AtomicBoolean retryScheduling = new AtomicBoolean(true);

    public Scheduler(List<DagNode> nodesToBeExecuted, IDagNodeExecutor dagNodeExecutor) {
        this.nodesToBeExecuted = nodesToBeExecuted;
        this.dagNodeExecutor = dagNodeExecutor;
        this.computeEngines = new PriorityQueue<>(
                Comparator.comparingInt(IComputeEngine::availableComputePower).reversed()
        );
    }

    public void start() {
        while (true) {
            // As tasks get submitted check that some node has compute power to execute it. Reject the task submission.

            for (DagNode dagNode : nodesToBeExecuted) { // Block as new tasks arrive.
                if (dagNode.isReadyForExecution()) {
                    IComputeEngine computeEngine = computeEngines.poll(); // try but don't block.
                    if(computeEngine.availableComputePower() > dagNode.cpuCost()) {
                        computeEngine.executeAsync(dagNode)
                                        .thenAccept((_ -> {
                                            retryScheduling.set(true);
                                            computeEngine.updateComputePower(-dagNode.cpuCost());
                                        }));
                        computeEngine.updateComputePower(dagNode.cpuCost());
                    }
                }
            }

            // Block here.
            retryScheduling.lock();

        }
    }

    public void addNewTaskForScheduling() {
        retryScheduling.set(true);
    }
}
