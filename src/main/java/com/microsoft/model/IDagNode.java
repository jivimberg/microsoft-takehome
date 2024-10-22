package com.microsoft.model;

public interface  IDagNode {

    Integer id();

    void execute();

    default int priority() {
        return 0;
    }
}
