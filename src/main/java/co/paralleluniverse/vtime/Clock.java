/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime;

import java.lang.management.RuntimeMXBean;

/**
 * Encapsulates the behavior of all JDK time-related operations.
 * <p>
 * Installing a clock via the {@link VirtualClock} class, will modify the operation of
 * {@link System#currentTimeMillis()}, {@link System#nanoTime()}, {@link Thread#sleep(long) Thread.sleep},
 * {@link Object#wait(long)} and any other operation relying on time or timeouts.
 *
 * @author pron
 */
public interface Clock {
    long System_currentTimeMillis();

    long System_nanoTime();

    long RuntimeMXBean_getStartTime(RuntimeMXBean runtimeMXBean);

    void Object_wait(Object obj, long timeout) throws InterruptedException;

    void Object_wait(Object obj, long timeout, int nanos) throws InterruptedException;

    void Thread_sleep(long millis) throws InterruptedException;

    void Thread_sleep(long millis, int nanos) throws InterruptedException;

    void Unsafe_park(sun.misc.Unsafe unsafe, boolean isAbsolute, long timeout);

    void afterGlobalClockSetup();
}
