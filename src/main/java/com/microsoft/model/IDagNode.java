package com.microsoft.model;

import com.microsoft.execution.IWork;

public interface  IDagNode extends IWork {

    Integer id();

    void execute();

    Integer cpuCost();
}
