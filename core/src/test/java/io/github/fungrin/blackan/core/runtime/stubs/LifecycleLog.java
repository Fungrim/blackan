package io.github.fungrin.blackan.core.runtime.stubs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LifecycleLog {

    private static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    public static void record(String event) {
        EVENTS.add(event);
    }

    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    public static void reset() {
        EVENTS.clear();
    }
}
