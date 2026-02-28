package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.context.RootContext;
import io.github.fungrim.blackan.injector.util.stubs.LifecycleBean;
import io.github.fungrim.blackan.injector.util.stubs.LifecycleOrderTracker;
import io.github.fungrim.blackan.injector.util.stubs.SecondLifecycleBean;

class LifecycleEventTest {

    private static final DotName PRIORITY = DotName.createSimple("jakarta.annotation.Priority");

    private final AtomicReference<Context> currentContext = new AtomicReference<>();

    private static int priorityOf(ClassInfo c) {
        AnnotationInstance ann = c.annotation(PRIORITY);
        return ann != null ? ann.value().asInt() : Integer.MAX_VALUE;
    }

    @Nested
    class BasicLifecycleEvents {

        @BeforeEach
        void setup() {
            LifecycleOrderTracker.reset();
        }

        @Test
        void initializedIsFiredOnContextCreation() throws IOException {
            RootContext root = RootContext.builder()
                    .withClasses(List.of(LifecycleBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            List<String> events = LifecycleOrderTracker.events();
            assertTrue(events.contains("LifecycleBean.initialized"));
        }

        @Test
        void beforeDestroyedAndDestroyedAreFiredOnClose() throws IOException {
            RootContext root = RootContext.builder()
                    .withClasses(List.of(LifecycleBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            LifecycleOrderTracker.reset();
            root.close();

            List<String> events = LifecycleOrderTracker.events();
            assertEquals(List.of("LifecycleBean.beforeDestroyed", "LifecycleBean.destroyed"), events);
        }

        @Test
        void allLifecycleEventsFireInCorrectOrder() throws IOException {
            RootContext root = RootContext.builder()
                    .withClasses(List.of(LifecycleBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            List<String> beforeClose = LifecycleOrderTracker.events();
            assertEquals(List.of("LifecycleBean.initialized"), beforeClose);

            root.close();

            List<String> all = LifecycleOrderTracker.events();
            assertEquals(List.of(
                    "LifecycleBean.initialized",
                    "LifecycleBean.beforeDestroyed",
                    "LifecycleBean.destroyed"), all);
        }
    }

    @Nested
    class OrderedLifecycleEvents {

        @BeforeEach
        void setup() {
            LifecycleOrderTracker.reset();
        }

        @Test
        void initializedEventsRespectEventOrdering() throws IOException {
            Comparator<ClassInfo> ordering = Comparator.comparingInt(LifecycleEventTest::priorityOf);

            RootContext root = RootContext.builder()
                    .withClasses(List.of(LifecycleBean.class, SecondLifecycleBean.class))
                    .withEventOrdering(ordering)
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            List<String> events = LifecycleOrderTracker.events();
            int secondIdx = events.indexOf("SecondLifecycleBean.initialized");
            int firstIdx = events.indexOf("LifecycleBean.initialized");
            assertTrue(secondIdx >= 0, "SecondLifecycleBean.initialized should be present");
            assertTrue(firstIdx >= 0, "LifecycleBean.initialized should be present");
            assertTrue(secondIdx < firstIdx,
                    "SecondLifecycleBean (@Priority(1)) should fire before LifecycleBean (no priority)");
        }
    }
}
