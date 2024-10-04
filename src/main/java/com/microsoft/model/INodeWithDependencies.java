package com.microsoft.model;

import java.util.Set;

public interface INodeWithDependencies {

    Integer getId();

    Set<? extends INodeWithDependencies> getDependencies();
}
