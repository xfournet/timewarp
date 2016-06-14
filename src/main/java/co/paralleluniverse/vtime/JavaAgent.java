/*
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime;

import java.lang.instrument.Instrumentation;
import java.util.*;
import co.paralleluniverse.vtime.clock.OffsetClock;
import co.paralleluniverse.vtime.clock.ScaledClock;
import co.paralleluniverse.vtime.clock.SystemClock;
import co.paralleluniverse.vtime.clock.manual.ManualClock;

public final class JavaAgent {
    private static final String INCLUDE_METHODS_KEY = "includesMethods";

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        // IMPORTANT : until the transformer is not setup avoid to use class that involves wrapped methods
        // eg: if Date is used before transformer setup, then new Date() will return the system time and not the wrapped time

        // IMPORTANT : some time related code should not be setup before the clock has been setup, else if can wait for ever
        // eg: TimerClock

        List<ConfEntry> conf = parseConfiguration(agentArguments);
        setupTransformer(instrumentation, conf);

        setupClock(conf);
    }

    private static List<ConfEntry> parseConfiguration(String agentArguments) {
        List<ConfEntry> conf = new ArrayList<>();
        if (agentArguments != null && !agentArguments.isEmpty()) {
            for (String param : agentArguments.split(",")) {
                String key = param;
                String value = "";
                int pos = param.indexOf("=");
                if (pos != -1) {
                    key = param.substring(0, pos);
                    value = param.substring(pos + 1);
                }
                conf.add(new ConfEntry(key.trim(), value.trim()));
            }
        }
        return conf;
    }

    private static void setupTransformer(Instrumentation instrumentation, List<ConfEntry> conf) {
        Set<String> includedMethods = null;
        Iterator<ConfEntry> it = conf.iterator();
        while (it.hasNext()) {
            ConfEntry e = it.next();
            if (INCLUDE_METHODS_KEY.equals(e.getKey())) {
                it.remove();
                if (includedMethods == null) {
                    includedMethods = new HashSet<>();
                }
                for (String includedMethod : e.getValue().split(":")) {
                    includedMethods.add(includedMethod.trim());
                }
            }
        }
        instrumentation.addTransformer(new VirtualTimeClassTransformer(includedMethods));
        Logger.info("Instrumentation transformer in place");
    }

    private static void setupClock(List<ConfEntry> conf) {
        Clock clock = SystemClock.instance();
        for (ConfEntry e : conf) {
            clock = processClockConfiguration(clock, e.getKey(), e.getValue());
        }
        VirtualClock.setGlobal(clock);
        clock.afterGlobalClockSetup();
        Logger.info("Using global clock %s", clock);
        Logger.info("Current system time is %s", new Date(System.currentTimeMillis()));
        Logger.info("Current virtual time is %s", new Date());
    }

    private static Clock processClockConfiguration(Clock clock, String clockType, String clockConf) {
        switch (clockType) {
            case "offset":
                return OffsetClock.create(clock, clockConf);

            case "scaled":
                return ScaledClock.create(clock, clockConf);

            case "manual":
                return ManualClock.create(clock, clockConf);

            default:
                Logger.warning("Unsupported clock type %s", clockType);
                return clock;
        }
    }

    private static class ConfEntry {
        private final String key;
        private final String value;

        ConfEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String getKey() {
            return key;
        }

        String getValue() {
            return value;
        }
    }

    private JavaAgent() {
    }
}
