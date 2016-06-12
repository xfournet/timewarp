/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime.instrumentation;

import java.lang.management.RuntimeMXBean;
import co.paralleluniverse.vtime.VirtualClock;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ClockProxy {
    private ClockProxy() {
    }

    public static long System_currentTimeMillis() {
        return VirtualClock.get().System_currentTimeMillis();
    }

    public static long System_nanoTime() {
        return VirtualClock.get().System_nanoTime();
    }

    public static long RuntimeMXBean_getStartTime(RuntimeMXBean runtimeMXBean) {
        return VirtualClock.get().RuntimeMXBean_getStartTime(runtimeMXBean);
    }

    public static void Object_wait(Object obj) throws InterruptedException {
        VirtualClock.get().Object_wait(obj, 0); // as per specification
    }

    public static void Object_wait(Object obj, long timeout) throws InterruptedException {
        VirtualClock.get().Object_wait(obj, timeout);
    }

    public static void Object_wait(Object obj, long timeout, int nanos) throws InterruptedException {
        VirtualClock.get().Object_wait(obj, timeout, nanos);
    }

    public static void Thread_sleep(long millis) throws InterruptedException {
        VirtualClock.get().Thread_sleep(millis);
    }

    public static void Thread_sleep(long millis, int nanos) throws InterruptedException {
        VirtualClock.get().Thread_sleep(millis, nanos);
    }

    public static void Unsafe_park(sun.misc.Unsafe unsafe, boolean isAbsolute, long timeout) {
        VirtualClock.get().Unsafe_park(unsafe, isAbsolute, timeout);
    }
}
