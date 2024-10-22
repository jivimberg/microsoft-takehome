package com.microsoft.model;

import java.util.concurrent.locks.ReentrantLock;

public final class Resource {
    private final String id;
    private final ReentrantLock lock;

    public Resource(String id) {
        this.id = id;
        lock = new ReentrantLock();
    }

    public boolean tryLock() {
        return lock.tryLock();
    }

    public void release() {
        if(lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public String id() {
        return id;
    }
}
