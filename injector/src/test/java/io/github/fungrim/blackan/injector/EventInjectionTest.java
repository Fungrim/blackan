package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.lookup.EventInjectionPoint;
import io.github.fungrim.blackan.injector.util.stubs.AppEvent;
import io.github.fungrim.blackan.injector.util.stubs.AsyncEventFiringBean;
import io.github.fungrim.blackan.injector.util.stubs.AsyncObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.EventFiringBean;
import io.github.fungrim.blackan.injector.util.stubs.QualifiedEventFiringBean;
import io.github.fungrim.blackan.injector.util.stubs.QualifiedEventObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.SyncObserverBean;
import jakarta.enterprise.event.NotificationOptions;

class EventInjectionTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();

    @Nested
    class InjectionPointResolution {

        @Test
        void eventFieldIsInjectedAsEventInjectionPoint() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(EventFiringBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            EventFiringBean bean = root.get(EventFiringBean.class);

            assertInstanceOf(EventInjectionPoint.class, bean.event,
                    "Injected Event<T> should be an EventInjectionPoint");
        }

        @Test
        void qualifiedEventFieldCarriesQualifierFromInjectionPoint() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(QualifiedEventFiringBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            QualifiedEventFiringBean bean = root.get(QualifiedEventFiringBean.class);

            assertInstanceOf(EventInjectionPoint.class, bean.event,
                    "@AppEvent Event<T> should be an EventInjectionPoint");
        }
    }

    @Nested
    class SyncFiring {

        @Test
        void fireDispatchesToSyncObservers() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(EventFiringBean.class, SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            EventFiringBean firing = root.get(EventFiringBean.class);
            SyncObserverBean observer = root.get(SyncObserverBean.class);

            firing.fire("hello via Event");

            assertEquals(List.of("hello via Event"), observer.received);
        }

        @Test
        void multipleFireCallsDispatchInOrder() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(EventFiringBean.class, SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            EventFiringBean firing = root.get(EventFiringBean.class);
            firing.fire("one");
            firing.fire("two");
            firing.fire("three");

            SyncObserverBean observer = root.get(SyncObserverBean.class);
            assertEquals(List.of("one", "two", "three"), observer.received);
        }
    }

    @Nested
    class AsyncFiring {

        @Test
        void fireAsyncDispatchesToAsyncObservers() throws Exception {
            Context root = Context.builder()
                    .withClasses(List.of(AsyncEventFiringBean.class, AsyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            AsyncEventFiringBean firing = root.get(AsyncEventFiringBean.class);
            AsyncObserverBean observer = root.get(AsyncObserverBean.class);

            firing.fire("async-msg").toCompletableFuture().get(5, TimeUnit.SECONDS);

            assertTrue(observer.latch.await(5, TimeUnit.SECONDS), "Async observer should complete");
            assertEquals(List.of("async-msg"), observer.received);
        }

        @Test
        void fireAsyncDoesNotDispatchToSyncObservers() throws Exception {
            Context root = Context.builder()
                    .withClasses(List.of(AsyncEventFiringBean.class, SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            AsyncEventFiringBean firing = root.get(AsyncEventFiringBean.class);
            firing.fire("async-msg").toCompletableFuture().get(5, TimeUnit.SECONDS);

            SyncObserverBean observer = root.get(SyncObserverBean.class);
            assertTrue(observer.received.isEmpty(),
                    "Sync observer must not receive async events");
        }
    }

    @Nested
    class QualifierPropagation {

        @Test
        void unqualifiedEventReachesUnqualifiedObserverOnly() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(
                            EventFiringBean.class,
                            SyncObserverBean.class,
                            QualifiedEventObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.get(EventFiringBean.class).fire("no-qualifier");

            assertEquals(List.of("no-qualifier"), root.get(SyncObserverBean.class).received,
                    "Unqualified observer should receive the event");
            assertTrue(root.get(QualifiedEventObserverBean.class).received.isEmpty(),
                    "@AppEvent observer must NOT receive an unqualified event");
        }

        @Test
        void qualifiedEventReachesBothQualifiedAndUnqualifiedObservers() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(
                            QualifiedEventFiringBean.class,
                            SyncObserverBean.class,
                            QualifiedEventObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.get(QualifiedEventFiringBean.class).fire("app-event");

            assertEquals(List.of("app-event"), root.get(SyncObserverBean.class).received,
                    "Unqualified observer should receive any event");
            assertEquals(List.of("app-event"), root.get(QualifiedEventObserverBean.class).received,
                    "@AppEvent observer should receive a matching qualified event");
        }
    }

    @Nested
    class SelectMethod {

        @Test
        void selectWithQualifierCreatesChildEventWithMergedQualifiers() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(
                            EventFiringBean.class,
                            SyncObserverBean.class,
                            QualifiedEventObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            EventFiringBean bean = root.get(EventFiringBean.class);

            bean.event.select(new AppEvent.Literal()).fire("selected");

            assertEquals(List.of("selected"), root.get(SyncObserverBean.class).received,
                    "Unqualified observer should still receive selected event");
            assertEquals(List.of("selected"), root.get(QualifiedEventObserverBean.class).received,
                    "@AppEvent observer should receive event fired via select(@AppEvent)");
        }

        @Test
        void selectWithSubtypeCreatesTypedChildEvent() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(EventFiringBean.class, SyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            EventFiringBean bean = root.get(EventFiringBean.class);
            bean.event.select(String.class).fire("subtype-fire");

            assertEquals(List.of("subtype-fire"), root.get(SyncObserverBean.class).received);
        }

        @Test
        void selectDoesNotMutateOriginalEvent() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(
                            EventFiringBean.class,
                            SyncObserverBean.class,
                            QualifiedEventObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            EventFiringBean bean = root.get(EventFiringBean.class);
            bean.event.select(new AppEvent.Literal()).fire("qualified");
            bean.event.fire("unqualified");

            assertEquals(List.of("qualified", "unqualified"),
                    root.get(SyncObserverBean.class).received);
            assertEquals(List.of("qualified"),
                    root.get(QualifiedEventObserverBean.class).received,
                    "Original unqualified event must not reach @AppEvent observer");
        }
    }

    @Nested
    class NotificationOptionsSupport {

        @Test
        void fireAsyncWithOptionsUsesCustomExecutor() throws Exception {
            Context root = Context.builder()
                    .withClasses(List.of(EventFiringBean.class, AsyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            CountDownLatch executorUsed = new CountDownLatch(1);
            Executor trackingExecutor = task -> {
                executorUsed.countDown();
                task.run();
            };
            NotificationOptions opts = NotificationOptions.ofExecutor(trackingExecutor);

            EventFiringBean bean = root.get(EventFiringBean.class);
            bean.event.fireAsync("opts-msg", opts).toCompletableFuture().get(5, TimeUnit.SECONDS);

            assertTrue(executorUsed.await(5, TimeUnit.SECONDS),
                    "Custom executor from NotificationOptions must be invoked");

            AsyncObserverBean observer = root.get(AsyncObserverBean.class);
            assertTrue(observer.latch.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("opts-msg"), observer.received);
        }

        @Test
        void fireAsyncWithNullOptionsUsesDefaultExecutor() throws Exception {
            Context root = Context.builder()
                    .withClasses(List.of(EventFiringBean.class, AsyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            EventFiringBean bean = root.get(EventFiringBean.class);
            bean.event.fireAsync("null-opts", null).toCompletableFuture().get(5, TimeUnit.SECONDS);

            AsyncObserverBean observer = root.get(AsyncObserverBean.class);
            assertTrue(observer.latch.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("null-opts"), observer.received);
        }

        @Test
        void contextFireAsyncWithOptionsUsesCustomExecutor() throws Exception {
            Context root = Context.builder()
                    .withClasses(List.of(AsyncObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            CountDownLatch executorUsed = new CountDownLatch(1);
            Executor trackingExecutor = task -> {
                executorUsed.countDown();
                task.run();
            };

            root.fireAsync("direct-opts", NotificationOptions.ofExecutor(trackingExecutor))
                    .toCompletableFuture().get(5, TimeUnit.SECONDS);

            assertTrue(executorUsed.await(5, TimeUnit.SECONDS),
                    "Custom executor must be used by Context.fireAsync(event, options)");

            AsyncObserverBean observer = root.get(AsyncObserverBean.class);
            assertTrue(observer.latch.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("direct-opts"), observer.received);
        }
    }
}
