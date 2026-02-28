package io.github.fungrim.blackan.injector.util.stubs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LifecycleOrderTracker {

    private static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    public static void record(Class<?> source, String event) {
        EVENTS.add(source.getSimpleName() + "." + event);
    }

    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    public static void reset() {
        EVENTS.clear();
    }
}
