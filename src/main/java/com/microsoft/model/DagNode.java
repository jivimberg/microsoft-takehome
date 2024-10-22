package com.microsoft.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record DagNode(Integer id, int priority) implements IDagNode {

    public DagNode(Integer id) {
        this(id, 0);
    }

    private final static Logger logger = LoggerFactory.getLogger(DagNode.class);

    @Override
    public void execute() {
        logger.info("Executing node: {}", id);
    }
}
