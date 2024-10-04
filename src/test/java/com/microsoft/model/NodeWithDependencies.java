package com.microsoft.model;

import java.util.Set;

public class NodeWithDependencies implements INodeWithDependencies {
    private final Integer id;
    private Set<INodeWithDependencies> dependencies;

    public NodeWithDependencies(int id, Set<INodeWithDependencies> dependencies) {
        this.id = id;
        this.dependencies = dependencies;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public Set<INodeWithDependencies> getDependencies() {
        return dependencies;
    }

    // Exposed to allow the creation of DAGs with cycles for testing
    public void setDependencies(Set<INodeWithDependencies> dependencies) {
        this.dependencies = dependencies;
    }
}