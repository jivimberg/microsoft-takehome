package com.microsoft.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public record DagNode(Integer id, List<Resource> resources) implements IDagNode {

    public DagNode(Integer id) {
        this(id, List.of());
    }

    private final static Logger logger = LoggerFactory.getLogger(DagNode.class);

    @Override
    public void execute() {
        logger.info("Executing node: {}", id);
    }
}
