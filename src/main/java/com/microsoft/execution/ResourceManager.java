package com.microsoft.execution;

import com.microsoft.model.Resource;

import java.util.Comparator;
import java.util.List;

class ResourceManager {
    public boolean acquireResources(List<Resource> resources) {
        // Sort resources to avoid deadlock
        resources.sort(Comparator.comparing(Resource::id));

        for (Resource resource : resources) {
            if (!resource.tryLock()) {
                releaseResources(resources);
                return false;
            }
        }
        return true; // All resources acquired successfully
    }

    public void releaseResources(List<Resource> resources) {
        for (Resource resource : resources) {
            resource.release();
        }
    }
}
