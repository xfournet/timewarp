package co.paralleluniverse.vtime;

public final class Logger {
    private static final String PREFIX = "VIRTUAL TIME: ";

    public static void info(String pattern, Object... args) {
        System.err.printf(PREFIX + pattern + "%n", args);
    }

    public static void warning(String pattern, Object... args) {
        System.err.printf(PREFIX + pattern + "%n", args);
    }

    public static void warning(String pattern, Throwable t, Object... args) {
        System.err.printf(PREFIX + pattern + "%n", args);
        t.printStackTrace();
    }

    private Logger() {
    }
}
