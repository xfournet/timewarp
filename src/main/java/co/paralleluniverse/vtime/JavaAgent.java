/*
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import co.paralleluniverse.vtime.clock.OffsetClock;
import co.paralleluniverse.vtime.clock.ScaledClock;
import co.paralleluniverse.vtime.clock.SystemClock;
import co.paralleluniverse.vtime.clock.manual.ManualClock;
import co.paralleluniverse.vtime.instrumentation.VirtualTimeClassTransformer;

public final class JavaAgent {

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        Map<String, String> args = parseArgs(agentArguments);
        setupTransformer(instrumentation, args.remove("includesMethods"));

        Clock clock = SystemClock.instance();
        for (Map.Entry<String, String> e : args.entrySet()) {
            clock = processArgument(clock, e.getKey(), e.getValue());
        }
        VirtualClock.setGlobal(clock);
        System.err.println("VIRTUAL TIME: using global clock " + clock + ". Current time is " + new Date());
    }

    private static Map<String, String> parseArgs(String agentArguments) {
        Map<String, String> args = new LinkedHashMap<>();
        if (agentArguments != null && !agentArguments.isEmpty()) {
            for (String param : agentArguments.split(",")) {
                String key = param;
                String value = "";
                int pos = param.indexOf("=");
                if (pos != -1) {
                    key = param.substring(0, pos);
                    value = param.substring(pos + 1);
                }
                args.put(key, value);
            }
        }
        return args;
    }

    private static void setupTransformer(Instrumentation instrumentation, String includesMethods) {
        Set<String> includedMethods = null;
        if (includesMethods != null) {
            includedMethods = new HashSet<>();
            for (String methodName : includesMethods.split(":")) {
                includedMethods.add(methodName);
            }
        }
        instrumentation.addTransformer(new VirtualTimeClassTransformer(includedMethods));
        System.err.println("VIRTUAL TIME: instrumentation transformer in place");
    }

    private static Clock processArgument(Clock clock, String key, String value) {
        switch (key) {
            case "offset":
                return processOffsetClockArg(clock, value);

            case "scaled":
                return processScaledClockArg(clock, value);

            case "manual":
                return processManualClockArg(clock, value);

            default:
                System.err.println("VIRTUAL TIME: unsupported clock param " + key);
                return clock;
        }
    }

    private static Clock processOffsetClockArg(Clock clock, String value) {
        long offset;
        if (value.startsWith("@")) {
            long startAt = parseDate(value.substring(1));
            offset = startAt - clock.RuntimeMXBean_getStartTime(ManagementFactory.getRuntimeMXBean());
        } else {
            offset = Long.parseLong(value);
        }
        return new OffsetClock(clock, offset);
    }

    private static Clock processScaledClockArg(Clock clock, String value) {
        double scale = Double.parseDouble(value);
        return new ScaledClock(clock, scale);
    }

    private static Clock processManualClockArg(Clock clock, String value) {
        long startTime;
        if (value.startsWith("@")) {
            startTime = parseDate(value.substring(1));
        } else {
            startTime = Long.parseLong(value);
        }
        return new ManualClock(startTime);
    }

    private static long parseDate(String date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        dateFormat.setLenient(true);
        try {
            return dateFormat.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private JavaAgent() {
    }
}
