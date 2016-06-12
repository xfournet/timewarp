/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime.clock;

import java.lang.management.RuntimeMXBean;
import co.paralleluniverse.vtime.Clock;

/**
 * The system clock.
 * This clock provides "real" time, as perceived by this running JVM.
 *
 * @author pron
 */
public final class SystemClock implements Clock {
    private static final SystemClock INSTANCE = new SystemClock();

    public static Clock instance() {
        return INSTANCE;
    }

    private SystemClock() {
    }

    @Override
    public String toString() {
        return "SystemClock";
    }

    @Override
    public long System_currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long System_nanoTime() {
        return System.nanoTime();
    }

    @Override
    public long RuntimeMXBean_getStartTime(RuntimeMXBean runtimeMXBean) {
        return runtimeMXBean.getStartTime();
    }

    @Override
    public void Object_wait(Object obj, long timeout) throws InterruptedException {
        obj.wait(timeout);
    }

    @Override
    public void Object_wait(Object obj, long timeout, int nanos) throws InterruptedException {
        obj.wait(timeout, nanos);
    }

    @Override
    public void Thread_sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Override
    public void Thread_sleep(long millis, int nanos) throws InterruptedException {
        Thread.sleep(millis, nanos);
    }

    @Override
    public void Unsafe_park(sun.misc.Unsafe unsafe, boolean isAbsolute, long timeout) {
        unsafe.park(isAbsolute, timeout);
    }
}
