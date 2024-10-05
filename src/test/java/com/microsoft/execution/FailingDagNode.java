package com.microsoft.execution;

import com.microsoft.model.IDagNode;

import java.util.concurrent.atomic.AtomicInteger;

public class FailingDagNode implements IDagNode {
    private final int id;
    private final int failures;
    private final AtomicInteger attempts = new AtomicInteger(0);

    public FailingDagNode(int id, int failures) {
        this.id = id;
        this.failures = failures;
    }

    @Override
    public Integer id() {
        return id;
    }

    @Override
    public void execute() {
        if (attempts.getAndIncrement() < failures) {
            throw new RuntimeException("Boom ??");
        }
    }

    public int getAttempts() {
        return attempts.get();
    }
}