package co.paralleluniverse.vtime.clock;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.*;
import co.paralleluniverse.vtime.Clock;
import co.paralleluniverse.vtime.Logger;

import static co.paralleluniverse.vtime.clock.Util.parseDate;

/**
 * Clock instance that is shifted from another clock.
 *
 * @author jleskovar
 */
public final class OffsetClock implements Clock {
    private static final long DEFAULT_WRITER_DELAY = 60 * 1000; // 1 minute

    public static Clock create(Clock clock, String conf) {
        long offset;
        State state = null;
        if (conf.startsWith("#")) {
            // offset is stored into state file
            conf = conf.substring(1);
            long writerDelay = DEFAULT_WRITER_DELAY;
            int pos = conf.indexOf("#");
            if (pos != -1) {
                writerDelay = Long.parseLong(conf.substring(pos + 1));
                conf = conf.substring(0, pos);
            }

            File file = new File(conf);
            state = new State(file, writerDelay);

            if (file.canRead()) {
                offset = state.readDate() - clock.RuntimeMXBean_getStartTime(ManagementFactory.getRuntimeMXBean());
            } else {
                offset = 0;
            }
        } else {
            if (conf.startsWith("@")) {
                // offset is a formatted date
                offset = parseDate(conf.substring(1)) - clock.RuntimeMXBean_getStartTime(ManagementFactory.getRuntimeMXBean());
            } else {
                // offset is a formatted date
                offset = Long.parseLong(conf);
            }
        }
        return new OffsetClock(clock, offset, state);
    }

    private final Clock source;
    private final long offset;
    private final State state;

    /**
     * Constructs a {@code OffsetClock} from the specified {@code Clock}.
     * The {@code offset} must be specified as milliseconds to be added from the {@code source}.
     *
     * @param source the source {@link Clock clock} from which to apply the offset
     * @param offset the offset duration in number of milliseconds
     * @param state the {@link State} if any or {@code null}
     */
    public OffsetClock(Clock source, long offset, State state) {
        this.source = source;
        this.offset = offset;
        this.state = state;
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

    @Override
    public void afterGlobalClockSetup() {
        source.afterGlobalClockSetup();
        if (state != null) {
            Timer timer = new Timer("OffsetClock state writer", true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    state.writeDate(System_currentTimeMillis());
                }
            }, 0, state.getWriterDelay());
        }
    }

    private static class State {
        private final File file;
        private final long writerDelay;

        State(File file, long writerDelay) {
            this.file = file;
            this.writerDelay = writerDelay;
        }

        long getWriterDelay() {
            return writerDelay;
        }

        long readDate() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                return Util.parseDate(br.readLine());
            } catch (IOException e) {
                Logger.warning("Unable to read offset clock state in file '%s' :", e, file.getAbsolutePath());
                throw new RuntimeException(e);
            }
        }

        void writeDate(long date) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                // write ahead time to ensure to not overlaps some clock time in case of crash
                pw.printf("%s%n", Util.formatDate(date + 2 * writerDelay));
            } catch (IOException e) {
                Logger.warning("Unable to save offset clock state in file '%s' :", e, file.getAbsolutePath());
            }
        }
    }
}
