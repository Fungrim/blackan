package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.context.RootContext;
import io.github.fungrim.blackan.injector.util.stubs.AppGreeting;
import io.github.fungrim.blackan.injector.util.stubs.AsyncObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.Greeting;
import io.github.fungrim.blackan.injector.util.stubs.ObserverWithInjectionBean;
import io.github.fungrim.blackan.injector.util.stubs.SyncObserverBean;

class ObserverTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();

    @Nested
    class SyncObservers {

        @Test
        void firesEventToSyncObserver() throws IOException {
            RootContext root = RootContext.builder()
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
            RootContext root = RootContext.builder()
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
            RootContext root = RootContext.builder()
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
            RootContext root = RootContext.builder()
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
            RootContext root = RootContext.builder()
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
            RootContext root = RootContext.builder()
                    .withClasses(List.of(SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.fireAsync("should-not-sync");

            SyncObserverBean bean = root.get(SyncObserverBean.class);
            assertTrue(bean.received.isEmpty());
        }
    }
}
