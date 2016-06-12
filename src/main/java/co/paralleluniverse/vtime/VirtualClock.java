/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime;

import co.paralleluniverse.vtime.clock.SystemClock;

/**
 * Sets or gets the virtual clock to be used by the system.
 *
 * @author pron
 */
public final class VirtualClock {
    private static Clock globalClock = SystemClock.instance(); // note that this isn't volatile

    /**
     * Puts the given clock in effect for the all threads
     * @param clock the {@link Clock} to be used for global clock
     */
    static void setGlobal(Clock clock) {
        globalClock = clock;
    }

    /**
     * @return the clock currently in effect for the current thread.
     */
    public static Clock get() {
        return globalClock;
    }

    private VirtualClock() {
    }
}
