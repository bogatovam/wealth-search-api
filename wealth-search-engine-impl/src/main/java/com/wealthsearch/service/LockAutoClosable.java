package com.wealthsearch.service;

import lombok.Data;
import lombok.Value;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Data
public class LockAutoClosable implements AutoCloseable{

    private final Lock lock;

    boolean isLocked = false;

    @Override
    public void close() {
        if (isLocked) {
            lock.unlock();
        }
    }

    public boolean tryLock(long time) throws InterruptedException {
        this.isLocked = lock.tryLock(time, TimeUnit.SECONDS);
        return isLocked;
    }
}
