package io.github.fungrim.blackan.bootstrap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class BootstrapLogger {

    private static final AtomicReference<BootstrapLogger> DEFAULT = new AtomicReference<>();

    public static BootstrapLogger of(String[] jvmArgs) {
        BootstrapLogger logger = new BootstrapLogger(jvmArgs);
        DEFAULT.set(logger);
        return logger;
    }

    public static BootstrapLogger instance() {
        return DEFAULT.get();
    }

    public static void log(String message) {
        instance().println(message);
    }

    private final boolean debug;

    private BootstrapLogger(String[] jvmArgs) {
        var list = new HashSet<>(Arrays.asList(jvmArgs));
        this.debug = list.contains("--debug") || list.contains("-d");
    }

    public void println(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
