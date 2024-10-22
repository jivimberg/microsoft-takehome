package com.microsoft.model;

import java.util.List;

public interface  IDagNode {

    Integer id();

    void execute();

    default List<Resource> resources() {
        return List.of();
    }
}
