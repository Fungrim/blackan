package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.util.stubs.BeanWithDependentDep;
import io.github.fungrim.blackan.injector.util.stubs.DependentLifecycleBean;
import io.github.fungrim.blackan.injector.util.stubs.LifecycleOrderTracker;
import io.github.fungrim.blackan.injector.util.stubs.OrderedDestroyDependent;
import io.github.fungrim.blackan.injector.util.stubs.OrderedDestroyParent;
import io.github.fungrim.blackan.injector.util.stubs.PostConstructBean;
import io.github.fungrim.blackan.injector.util.stubs.SessionLifecycleBean;

class BeanLifecycleTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();

    @Nested
    class PostConstructTests {

        @Test
        void postConstructIsCalledAfterInjection() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(PostConstructBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            PostConstructBean bean = root.get(PostConstructBean.class);
            assertTrue(bean.postConstructCalled);
        }

        @Test
        void postConstructIsCalledOnDependentBean() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(DependentLifecycleBean.class, BeanWithDependentDep.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            BeanWithDependentDep owner = root.get(BeanWithDependentDep.class);
            assertTrue(owner.dependent.postConstructCalled);
        }
    }

    @Nested
    class PreDestroyTests {

        @Test
        void preDestroyIsCalledOnContextClose() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(PostConstructBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            PostConstructBean bean = root.get(PostConstructBean.class);
            assertFalse(bean.preDestroyCalled);

            root.close();
            assertTrue(bean.preDestroyCalled);
        }

        @Test
        void preDestroyIsCalledOnDependentBeanWhenContextCloses() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(DependentLifecycleBean.class, BeanWithDependentDep.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            BeanWithDependentDep owner = root.get(BeanWithDependentDep.class);
            assertFalse(owner.dependent.preDestroyCalled);

            root.close();
            assertTrue(owner.dependent.preDestroyCalled);
        }

        @Test
        void preDestroyIsCalledOnSessionScopedBeanWhenSessionCloses() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(SessionLifecycleBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);

            SessionLifecycleBean bean = session.get(SessionLifecycleBean.class);
            assertTrue(bean.postConstructCalled);
            assertFalse(bean.preDestroyCalled);

            session.close();
            assertTrue(bean.preDestroyCalled);
        }
    }

    @Nested
    class DestructionOrdering {

        @BeforeEach
        void setup() {
            LifecycleOrderTracker.reset();
        }

        @Test
        void normalScopeBeanIsDestroyedBeforeItsDependentDependency() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(OrderedDestroyParent.class, OrderedDestroyDependent.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.get(OrderedDestroyParent.class);
            root.close();

            List<String> events = LifecycleOrderTracker.events();
            int parentIdx = events.indexOf("OrderedDestroyParent.preDestroy");
            int dependentIdx = events.indexOf("OrderedDestroyDependent.preDestroy");
            assertTrue(parentIdx >= 0, "OrderedDestroyParent.preDestroy should be present");
            assertTrue(dependentIdx >= 0, "OrderedDestroyDependent.preDestroy should be present");
            assertTrue(parentIdx < dependentIdx,
                    "Normal-scope ancestor @PreDestroy should fire before dependent bean @PreDestroy, " +
                    "but got order: " + events);
        }

        @Test
        void destructionOrderIsReverseOfCreationOrder() throws IOException {
            Context root = Context.builder()
                    .withClasses(List.of(OrderedDestroyParent.class, OrderedDestroyDependent.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(root);

            root.get(OrderedDestroyParent.class);
            root.close();

            assertEquals(
                    List.of("OrderedDestroyParent.preDestroy", "OrderedDestroyDependent.preDestroy"),
                    LifecycleOrderTracker.events());
        }
    }
}
