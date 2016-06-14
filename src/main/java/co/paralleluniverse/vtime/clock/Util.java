package co.paralleluniverse.vtime.clock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Util {
    private static final String DATE_FORMAT = "yyyyMMdd'T'HHmmss";

    public static long parseDate(String date) {
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatDate(long date) {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date(date));
    }

    private Util() {
    }
}
