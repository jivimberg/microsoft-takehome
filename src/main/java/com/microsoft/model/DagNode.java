package com.microsoft.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public final class DagNode implements IDagNode {

    private final static Logger logger = LoggerFactory.getLogger(DagNode.class);
    private final Integer id;
    private final Integer numberOfDependencies;
    private Integer completedDependencies;
    private final List<DagNode> dependents;

    public DagNode(Integer id, Integer numberOfDependencies, List<DagNode> dependents) {
        this.id = id;
        this.numberOfDependencies = numberOfDependencies;
        this.dependents = dependents;
    }

    private Integer indegree;

    @Override
    public void execute() {
        logger.info("Executing node: {}", id);
        notifyDependents();
    }

    @Override
    public Integer cpuCost() {
        return 0;
    }

    public void notifyDependents() {
        for (DagNode dependent : dependents) {
            dependent.dependencyCompleted();
        }
    }

    public void dependencyCompleted() {
        completedDependencies++;
    }

    public boolean isReadyForExecution() {
        return completedDependencies == numberOfDependencies;
    }

    @Override
    public Integer id() {
        return id;
    }

    public List<DagNode> dependents() {
        return dependents;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DagNode) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.indegree, that.indegree) &&
                Objects.equals(this.dependents, that.dependents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, indegree, dependents);
    }

    @Override
    public String toString() {
        return "DagNode[" +
                "id=" + id + ", " +
                "indegree=" + indegree + ", " +
                "dependents=" + dependents + ']';
    }

}
