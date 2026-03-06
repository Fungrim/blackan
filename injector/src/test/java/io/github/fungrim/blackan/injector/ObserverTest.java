package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.util.stubs.AppGreeting;
import io.github.fungrim.blackan.injector.util.stubs.AsyncObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.ClassHighPriorityObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.ClassLowPriorityObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.EventOrderTracker;
import io.github.fungrim.blackan.injector.util.stubs.Greeting;
import io.github.fungrim.blackan.injector.util.stubs.HighPriorityObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.LowPriorityObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.ObserverWithInjectionBean;
import io.github.fungrim.blackan.injector.util.stubs.SyncObserverBean;

class ObserverTest {

    private static final DotName PRIORITY = DotName.createSimple("jakarta.annotation.Priority");

    private final AtomicReference<Context> currentContext = new AtomicReference<>();

    private static int classPriorityOf(ClassInfo c) {
        AnnotationInstance ann = c.annotation(PRIORITY);
        return ann != null ? ann.value().asInt() : Integer.MAX_VALUE;
    }

    @Nested
    class SyncObservers {

        @Test
        void firesEventToSyncObserver() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fire("hello");

            SyncObserverBean bean = root.get(SyncObserverBean.class);
            assertEquals(List.of("hello"), bean.received);
        }

        @Test
        void firesMultipleEventsToSyncObserver() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fire("one");
            root.fire("two");

            SyncObserverBean bean = root.get(SyncObserverBean.class);
            assertEquals(List.of("one", "two"), bean.received);
        }

        @Test
        void observerMethodCanHaveInjectedParameters() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(
                            Greeting.class, AppGreeting.class,
                            ObserverWithInjectionBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fire("test");

            ObserverWithInjectionBean bean = root.get(ObserverWithInjectionBean.class);
            assertEquals(List.of("test:hello from app"), bean.received);
        }

        @Test
        void nonMatchingEventTypeIsNotObserved() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fire(42);

            SyncObserverBean bean = root.get(SyncObserverBean.class);
            assertTrue(bean.received.isEmpty());
        }
    }

    @Nested
    class AsyncObservers {

        @Test
        void firesEventToAsyncObserver() throws Exception {
            Context root = Context.builder()
                    .withClasses(List.of(AsyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            AsyncObserverBean bean = root.get(AsyncObserverBean.class);
            root.fireAsync("async-hello");

            assertTrue(bean.latch.await(5, TimeUnit.SECONDS), "Async observer should complete within timeout");
            assertEquals(List.of("async-hello"), bean.received);
        }

        @Test
        void syncObserverDoesNotReceiveAsyncEvent() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fireAsync("should-not-sync");

            SyncObserverBean bean = root.get(SyncObserverBean.class);
            assertTrue(bean.received.isEmpty());
        }
    }

    @Nested
    class ObserverOrdering {

        @BeforeEach
        void setup() {
            EventOrderTracker.reset();
        }

        @Test
        void fireRespectsCdiPriorityOnEventParameter() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(HighPriorityObserverBean.class, LowPriorityObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fire("test");

            List<String> order = EventOrderTracker.events();
            assertEquals(List.of("HighPriorityObserverBean", "LowPriorityObserverBean"), order,
                    "Observer with @Priority(100) should fire before @Priority(2000)");
        }

    }

    @Nested
    class CustomOrderedObservers {

        @BeforeEach
        void setup() {
            EventOrderTracker.reset();
        }

        @Test
        void fireInCustomOrderUsesProvidedComparator() throws IOException {
            Comparator<ClassInfo> ordering = Comparator.comparingInt(ObserverTest::classPriorityOf);

            Context root = Context.builder()
                    .withClasses(List.of(ClassHighPriorityObserverBean.class, ClassLowPriorityObserverBean.class))
                    .withCustomEventOrdering(ordering)
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fireInCustomOrder("test");

            List<String> order = EventOrderTracker.events();
            assertEquals(List.of("ClassHighPriorityObserverBean", "ClassLowPriorityObserverBean"), order,
                    "fireInCustomOrder should use provided comparator: class @Priority(1) before @Priority(100)");
        }

        @Test
        void fireInReverseCustomOrderUsesReversedComparator() throws IOException {
            Comparator<ClassInfo> ordering = Comparator.comparingInt(ObserverTest::classPriorityOf);

            Context root = Context.builder()
                    .withClasses(List.of(ClassHighPriorityObserverBean.class, ClassLowPriorityObserverBean.class))
                    .withCustomEventOrdering(ordering)
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fireInReverseCustomOrder("test");

            List<String> order = EventOrderTracker.events();
            assertEquals(List.of("ClassLowPriorityObserverBean", "ClassHighPriorityObserverBean"), order,
                    "fireInReverseCustomOrder should reverse the comparator: @Priority(100) before @Priority(1)");
        }

        @Test
        void fireDoesNotUseCustomOrderingComparator() throws IOException {
            Comparator<ClassInfo> ordering = Comparator.comparingInt(ObserverTest::classPriorityOf);

            Context root = Context.builder()
                    .withClasses(List.of(HighPriorityObserverBean.class, LowPriorityObserverBean.class))
                    .withCustomEventOrdering(ordering)
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fire("test");

            List<String> order = EventOrderTracker.events();
            assertEquals(List.of("HighPriorityObserverBean", "LowPriorityObserverBean"), order,
                    "fire() must use CDI spec priority on event parameter, not the custom class comparator");
        }
    }
}
