package com.microsoft.model;

import java.util.Set;

public interface INodeWithDependencies {

    Integer id();

    Set<? extends INodeWithDependencies> dependencies();
}
