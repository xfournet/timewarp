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
 * A clock providing scaled time (slowed down or sped up) relative to another clock.
 *
 * @author pron
 */
public final class ScaledClock implements Clock {
    private final int NANO_MILLIS = 1000 * 1000;

    private final Clock source;
    private final double scale;

    private final long startTime;
    private final long startNanos;

    /**
     * Constructs a {@code ScaledClock} of the a given clock.
     * A scale {@literal >} 1 would make this clock run faster relative to the given clock;
     * a scale {@literal <} 1 would make this clock run slower relative to the given clock.
     *
     * @param source the {@link Clock} to be used as source
     * @param scale the scale by which the given clock's time is scaled; must be positive.
     */
    public ScaledClock(Clock source, double scale) {
        if (scale <= 0.0) {
            throw new IllegalArgumentException("Scale must be positive; was " + scale);
        }
        this.source = source;
        this.scale = scale;

        this.startTime = source.System_currentTimeMillis();
        this.startNanos = source.System_nanoTime();
    }

    @Override
    public String toString() {
        return "ScaledClock{source=" + source + " scale=" + scale + '}';
    }

    @Override
    public long System_currentTimeMillis() {
        return startTime + (long) ((source.System_currentTimeMillis() - startTime) * scale);
    }

    @Override
    public long System_nanoTime() {
        return startNanos + (long) ((source.System_nanoTime() - startNanos) * scale); // we use startNanos just to keep the scaled number smaller
    }

    @Override
    public long RuntimeMXBean_getStartTime(RuntimeMXBean runtimeMXBean) {
        return startTime + (long) ((source.RuntimeMXBean_getStartTime(runtimeMXBean) - startTime) * scale);
    }

    @Override
    public void Object_wait(Object obj, long timeout) throws InterruptedException {
        source.Object_wait(obj, (long) (timeout / scale));
    }

    @Override
    public void Object_wait(Object obj, long timeout, int nanos) throws InterruptedException {
        long totalNanos = (long) ((timeout * NANO_MILLIS + nanos) / scale);
        source.Object_wait(obj, totalNanos / NANO_MILLIS, (int) (totalNanos % NANO_MILLIS));
    }

    @Override
    public void Thread_sleep(long millis) throws InterruptedException {
        source.Thread_sleep((long) (millis / scale));
    }

    @Override
    public void Thread_sleep(long millis, int nanos) throws InterruptedException {
        long totalNanos = (long) ((millis * NANO_MILLIS + nanos) / scale);
        source.Thread_sleep(totalNanos / NANO_MILLIS, (int) (totalNanos % NANO_MILLIS));
    }

    @Override
    public void Unsafe_park(sun.misc.Unsafe unsafe, boolean isAbsolute, long timeout) {
        if (timeout <= 0) {
            unsafe.park(isAbsolute, timeout);
            return;
        }
        if (!isAbsolute) {
            source.Unsafe_park(unsafe, isAbsolute, (long) (timeout / scale));
        } else {
            source.Unsafe_park(unsafe, isAbsolute, source.System_currentTimeMillis() + (long) ((timeout - System_currentTimeMillis()) / scale));
        }
    }
}
