/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime.clock.manual;

import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import co.paralleluniverse.vtime.Clock;
import co.paralleluniverse.vtime.Logger;

import static co.paralleluniverse.vtime.clock.Util.parseDate;

/**
 * A clock that only progresses when its time is manually advanced by calls to {@link #advance(long, TimeUnit) advance}.
 *
 * @author pron
 */
public final class ManualClock implements Clock {

    public static Clock create(Clock clock, String conf) {
        Logger.info("Manual clock ignore previous clock %s", clock);
        long startTime;
        if (conf.startsWith("@")) {
            startTime = parseDate(conf.substring(1));
        } else {
            startTime = Long.parseLong(conf);
        }
        return new ManualClock(startTime);
    }

    private final Queue<Scheduled> waiters = new ConcurrentSkipListPriorityQueue<>();
    private final long startTime;
    private volatile long nanos;

    /**
     * Creates a new {@code ManualClock} instance.
     *
     * @param startTime the initial time which will be returned by {@code System.currentTimeMillis()}.
     */
    public ManualClock(long startTime) {
        if (startTime < 0) {
            throw new IllegalArgumentException("startTime must be >= 0; was " + startTime);
        }
        this.startTime = startTime;
        this.nanos = 0;
    }

    @Override
    public String toString() {
        return "ManualClock{startTime=" + startTime + " nanos=" + nanos + '}';
    }

    /**
     * Advances this clock's time by the given duration.
     *
     * @param duration the time duration
     * @param unit the time duration's unit
     */
    public synchronized void advance(long duration, TimeUnit unit) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive; was " + duration);
        }

        this.nanos += unit.toNanos(duration);

        for (; ; ) {
            Scheduled s = waiters.peek();
            if (s == null || s.deadline > nanos) {
                break;
            }
            // at this point we know there are runnable waiters
            // new ones won't be added because we've already advanced nanos
            waiters.poll().wakeup();
        }
    }

    @Override
    public long System_currentTimeMillis() {
        return startTime + TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    @Override
    public long System_nanoTime() {
        return nanos;
    }

    @Override
    public long RuntimeMXBean_getStartTime(RuntimeMXBean runtimeMXBean) {
        return startTime;
    }

    @Override
    public void Object_wait(Object obj, long timeout) throws InterruptedException {
        if (timeout <= 0) {
            obj.wait(timeout);
        } else {
            final long deadline = nanos + TimeUnit.MILLISECONDS.toNanos(timeout);
            try {
                InterruptScheduled s = interrupt(deadline, Thread.currentThread());
                waiters.add(s);
                obj.wait();

                synchronized (this) {
                    if (deadline < nanos) {
                        s.disable();
                    } else // advance was called between obj.wait and this synchronized block
                    {
                        Thread.interrupted();
                    }
                }
            } catch (InterruptedException e) {
                handleInterrupted(deadline, e);
            }
        }
    }

    @Override
    public void Object_wait(Object obj, long timeout, int nanos) throws InterruptedException {
        Object_wait(obj, timeout);
    }

    @Override
    public void Thread_sleep(long millis) throws InterruptedException {
        if (millis <= 0) {
            Thread.sleep(millis);
        } else {
            final long deadline = nanos + TimeUnit.MILLISECONDS.toNanos(millis);
            try {
                waiters.add(interrupt(deadline, Thread.currentThread()));
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                handleInterrupted(deadline, e);
            }
        }
    }

    @Override
    public void Thread_sleep(long millis, int nanos) throws InterruptedException {
        Thread_sleep(millis);
    }

    @Override
    public void Unsafe_park(sun.misc.Unsafe unsafe, boolean isAbsolute, long timeout) {
        if (timeout <= 0) {
            unsafe.park(isAbsolute, nanos);
        } else {
            final long deadline = nanos + (isAbsolute ? TimeUnit.MILLISECONDS.toNanos(timeout - System_currentTimeMillis()) : timeout);
            waiters.add(unpark(deadline, Thread.currentThread()));
            if (nanos < deadline) {
                unsafe.park(false, 0L);
            }
        }
    }

    @Override
    public void afterGlobalClockSetup() {
    }

    private void handleInterrupted(long deadline, InterruptedException e) throws InterruptedException {
        if (nanos < deadline) {
            throw e;
        }
        Thread.interrupted();
    }

    private abstract static class Scheduled implements Comparable<Scheduled> {
        final long deadline;
        final Thread thread;

        Scheduled(long deadline, Thread thread) {
            this.deadline = deadline;
            this.thread = thread;
        }

        @Override
        public int compareTo(Scheduled o) {
            return signum(deadline - o.deadline);
        }

        public abstract void wakeup();
    }

    private static int signum(long x) {
        long y = (x & 0x7fffffffffffffffL) + 0x7fffffffffffffffL;
        return (int) ((x >> 63) | (y >>> 63));
    }

    private Scheduled unpark(long deadline, Thread t) {
        return new Scheduled(deadline, t) {
            @Override
            public void wakeup() {
                LockSupport.unpark(thread);
            }
        };
    }

    private InterruptScheduled interrupt(long deadline, Thread t) {
        return new InterruptScheduled(deadline, t);
    }

    private static class InterruptScheduled extends Scheduled {
        private volatile boolean disabled;

        InterruptScheduled(long deadline, Thread thread) {
            super(deadline, thread);
        }

        void disable() {
            disabled = true;
        }

        @Override
        public void wakeup() {
            if (!disabled) {
                thread.interrupt();
            }
        }
    }
}
