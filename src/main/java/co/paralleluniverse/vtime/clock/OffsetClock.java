package co.paralleluniverse.vtime.clock;

import java.lang.management.RuntimeMXBean;
import co.paralleluniverse.vtime.Clock;

/**
 * Clock instance that is shifted from another clock.
 *
 * @author jleskovar
 */
public final class OffsetClock implements Clock {
    private final Clock source;
    private final long offset;

    /**
     * Constructs a {@code OffsetClock} from the specified {@code Clock}.
     * The {@code offset} must be specified as milliseconds to be added from the {@code source}.
     *
     * @param source the base {@link Clock clock} from which to apply the offset
     * @param offset the offset duration in number of milliseconds
     */
    public OffsetClock(Clock source, long offset) {
        this.source = source;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "OffsetClock{source=" + source + " offset=" + offset + '}';
    }

    @Override
    public long System_currentTimeMillis() {
        return source.System_currentTimeMillis() + offset;
    }

    @Override
    public long System_nanoTime() {
        // since this can be used only for measuring delta, no need to offset this method
        return source.System_nanoTime();
    }

    @Override
    public long RuntimeMXBean_getStartTime(RuntimeMXBean runtimeMXBean) {
        return source.RuntimeMXBean_getStartTime(runtimeMXBean) + offset;
    }

    @Override
    public void Object_wait(Object obj, long timeout) throws InterruptedException {
        source.Object_wait(obj, timeout);
    }

    @Override
    public void Object_wait(Object obj, long timeout, int nanos) throws InterruptedException {
        source.Object_wait(obj, timeout, nanos);
    }

    @Override
    public void Thread_sleep(long millis) throws InterruptedException {
        source.Thread_sleep(millis);
    }

    @Override
    public void Thread_sleep(long millis, int nanos) throws InterruptedException {
        source.Thread_sleep(millis, nanos);
    }

    @Override
    public void Unsafe_park(sun.misc.Unsafe unsafe, boolean isAbsolute, long timeout) {
        source.Unsafe_park(unsafe, isAbsolute, timeout);
    }
}
