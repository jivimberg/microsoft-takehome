package com.microsoft.parser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.model.INodeWithDependencies;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public record DagXml(List<Node> nodes) {
    @JsonCreator
    public DagXml(@JsonProperty(required = true, value = "Nodes") List<Node> nodes) {
        this.nodes = nodes;
    }

    public record Node(Integer id, Set<Node> dependencies) implements INodeWithDependencies {
        @JsonCreator
        public Node(@JsonProperty(required = true, value =  "Id") Integer id,
                    @JsonProperty(value = "dependencies") Set<Node> dependencies
        ) {
            this.id = id;
            this.dependencies = dependencies != null ? dependencies : Collections.emptySet();
            validateDependencies();
        }

        private void validateDependencies() {
            for (Node dependency : dependencies) {
                if (!dependency.dependencies().isEmpty()) {
                    throw new IllegalArgumentException("Dependencies of node " + id + " should not have their own dependencies.");
                }
            }
        }
    }
}