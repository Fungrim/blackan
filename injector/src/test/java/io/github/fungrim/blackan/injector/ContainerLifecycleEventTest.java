package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.common.cdi.AfterTypeDiscovery;
import io.github.fungrim.blackan.common.cdi.BeforeBeanDiscovery;
import io.github.fungrim.blackan.common.cdi.ContainerListener;
import io.github.fungrim.blackan.common.cdi.ProcessAnnotatedType;
import io.github.fungrim.blackan.injector.util.stubs.AfterBeanDiscoveryObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.AfterDeploymentValidationObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.BeforeShutdownObserverBean;
import io.github.fungrim.blackan.injector.util.stubs.ContainerInitEventTracker;
import io.github.fungrim.blackan.injector.util.stubs.ProcessObserverMethodVetoBean;
import io.github.fungrim.blackan.injector.util.stubs.VetoableTypeBean;
import io.github.fungrim.blackan.injector.util.stubs.VetoedObserverTargetBean;

class ContainerLifecycleEventTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();

    @BeforeEach
    void reset() {
        ContainerInitEventTracker.reset();
    }

    private Context buildWith(Class<?>... classes) throws IOException {
        return Context.builder()
                .withClasses(List.of(classes))
                .withScopeProvider(() -> currentContext.get())
                .build();
    }

    // -------------------------------------------------------------------------
    // Post-scan events via @Observes
    // -------------------------------------------------------------------------

    @Nested
    class PostScanEvents {

        @Test
        void afterBeanDiscoveryIsObservable() throws IOException {
            Context ctx = buildWith(AfterBeanDiscoveryObserverBean.class);
            currentContext.set(ctx);
            assertTrue(ContainerInitEventTracker.events().contains("afterBeanDiscovery"));
        }

        @Test
        void afterDeploymentValidationIsObservable() throws IOException {
            Context ctx = buildWith(AfterDeploymentValidationObserverBean.class);
            currentContext.set(ctx);
            assertTrue(ContainerInitEventTracker.events().contains("afterDeploymentValidation"));
        }

        @Test
        void afterDeploymentValidationFiresAfterAfterBeanDiscovery() throws IOException {
            Context ctx = buildWith(
                    AfterBeanDiscoveryObserverBean.class,
                    AfterDeploymentValidationObserverBean.class);
            currentContext.set(ctx);
            List<String> events = ContainerInitEventTracker.events();
            int abd = events.indexOf("afterBeanDiscovery");
            int adv = events.indexOf("afterDeploymentValidation");
            assertTrue(abd >= 0, "AfterBeanDiscovery must fire");
            assertTrue(adv >= 0, "AfterDeploymentValidation must fire");
            assertTrue(abd < adv, "AfterBeanDiscovery must fire before AfterDeploymentValidation");
        }

        @Test
        void postScanEventsDoNotFireForSubcontexts() throws IOException {
            Context root = buildWith(
                    AfterBeanDiscoveryObserverBean.class,
                    AfterDeploymentValidationObserverBean.class);
            currentContext.set(root);
            int countAfterRoot = ContainerInitEventTracker.events().size();

            Context sub = root.subcontext(Scope.SESSION);
            currentContext.set(sub);

            assertEquals(countAfterRoot, ContainerInitEventTracker.events().size(),
                    "Post-scan events must not fire again for subcontexts");
            sub.close();
        }
    }

    // -------------------------------------------------------------------------
    // Shutdown events via @Observes
    // -------------------------------------------------------------------------

    @Nested
    class ShutdownEvents {

        @Test
        void beforeShutdownFiresOnRootClose() throws IOException {
            Context ctx = buildWith(BeforeShutdownObserverBean.class);
            currentContext.set(ctx);
            ctx.close();
            assertTrue(ContainerInitEventTracker.events().contains("beforeShutdown"));
        }

        @Test
        void beforeShutdownFiresBeforeBeforeDestroyed() throws IOException {
            Context ctx = buildWith(BeforeShutdownObserverBean.class);
            currentContext.set(ctx);
            ctx.close();
            List<String> events = ContainerInitEventTracker.events();
            int shutdown = events.indexOf("beforeShutdown");
            int beforeDestroyed = events.indexOf("beforeDestroyed");
            assertTrue(shutdown >= 0, "BeforeShutdown must be recorded");
            assertTrue(beforeDestroyed >= 0, "BeforeDestroyed must be recorded");
            assertTrue(shutdown < beforeDestroyed,
                    "BeforeShutdown must fire before BeforeDestroyed");
        }

        @Test
        void beforeShutdownDoesNotFireForSubcontextClose() throws IOException {
            Context root = buildWith(BeforeShutdownObserverBean.class);
            currentContext.set(root);

            Context sub = root.subcontext(Scope.SESSION);
            currentContext.set(sub);
            sub.close();

            assertFalse(ContainerInitEventTracker.events().contains("beforeShutdown"),
                    "BeforeShutdown must not fire when only a subcontext closes");
        }
    }

    // -------------------------------------------------------------------------
    // ProcessAnnotatedType veto via ContainerListener
    // -------------------------------------------------------------------------

    @Nested
    class ProcessAnnotatedTypeVeto {

        @Test
        void vetoedTypeIsUnsatisfied() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(VetoableTypeBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .withListener(new ContainerListener() {
                        @Override
                        public void processAnnotatedType(ProcessAnnotatedType event) {
                            if (event.type().name().toString().equals(VetoableTypeBean.class.getName())) {
                                event.veto();
                            }
                        }
                    })
                    .build();
            currentContext.set(ctx);
            assertTrue(ctx.getInstance(VetoableTypeBean.class).isUnsatisfied(),
                    "Vetoed type must not be resolvable");
        }

        @Test
        void nonVetoedTypesAreUnaffected() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(VetoableTypeBean.class, AfterBeanDiscoveryObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .withListener(new ContainerListener() {
                        @Override
                        public void processAnnotatedType(ProcessAnnotatedType event) {
                            if (event.type().name().toString().equals(VetoableTypeBean.class.getName())) {
                                event.veto();
                            }
                        }
                    })
                    .build();
            currentContext.set(ctx);
            assertFalse(ctx.getInstance(AfterBeanDiscoveryObserverBean.class).isUnsatisfied(),
                    "Non-vetoed type must still be resolvable");
        }
    }

    // -------------------------------------------------------------------------
    // ProcessObserverMethod veto via @Observes
    // -------------------------------------------------------------------------

    @Nested
    class ProcessObserverMethodVeto {

        @Test
        void vetoedObserverDoesNotReceiveEvents() throws IOException {
            Context ctx = buildWith(
                    ProcessObserverMethodVetoBean.class,
                    VetoedObserverTargetBean.class);
            currentContext.set(ctx);
            ctx.fire("test-event");
            assertFalse(ContainerInitEventTracker.events().contains("vetoedObserverFired"),
                    "Vetoed observer must not receive events");
        }

        @Test
        void nonVetoedObserversAreUnaffected() throws IOException {
            Context ctx = buildWith(
                    ProcessObserverMethodVetoBean.class,
                    VetoedObserverTargetBean.class);
            currentContext.set(ctx);
            ctx.fire("test-event");
            assertFalse(ContainerInitEventTracker.events().contains("vetoedObserverFired"),
                    "VetoedObserverTargetBean should not have fired");
        }
    }

    // -------------------------------------------------------------------------
    // ContainerListener pre-scan callbacks
    // -------------------------------------------------------------------------

    @Nested
    class ContainerListenerCallbacks {

        @Test
        void beforeBeanDiscoveryFiresFirst() throws IOException {
            List<String> log = new ArrayList<>();
            Context.builder()
                    .withClasses(List.of(VetoableTypeBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .withListener(new ContainerListener() {
                        @Override
                        public void beforeBeanDiscovery(BeforeBeanDiscovery event) {
                            log.add("bbd");
                        }
                        @Override
                        public void processAnnotatedType(ProcessAnnotatedType event) {
                            log.add("pat");
                        }
                    })
                    .build();
            assertFalse(log.isEmpty(), "ContainerListener must have been called");
            assertEquals("bbd", log.get(0),
                    "BeforeBeanDiscovery must be the first ContainerListener callback");
        }

        @Test
        void processAnnotatedTypeFiresForEachIndexedClass() throws IOException {
            List<String> visited = new ArrayList<>();
            Context.builder()
                    .withClasses(List.of(VetoableTypeBean.class, AfterBeanDiscoveryObserverBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .withListener(new ContainerListener() {
                        @Override
                        public void processAnnotatedType(ProcessAnnotatedType event) {
                            visited.add(event.type().name().toString());
                        }
                    })
                    .build();
            assertTrue(visited.contains(VetoableTypeBean.class.getName()),
                    "ProcessAnnotatedType must fire for VetoableTypeBean");
            assertTrue(visited.contains(AfterBeanDiscoveryObserverBean.class.getName()),
                    "ProcessAnnotatedType must fire for AfterBeanDiscoveryObserverBean");
        }

        @Test
        void afterTypeDiscoveryFiresAfterAllProcessAnnotatedTypeCallbacks() throws IOException {
            List<String> log = new ArrayList<>();
            Context.builder()
                    .withClasses(List.of(VetoableTypeBean.class))
                    .withScopeProvider(() -> currentContext.get())
                    .withListener(new ContainerListener() {
                        @Override
                        public void processAnnotatedType(ProcessAnnotatedType event) {
                            log.add("pat");
                        }
                        @Override
                        public void afterTypeDiscovery(AfterTypeDiscovery event) {
                            log.add("atd");
                        }
                    })
                    .build();
            assertFalse(log.isEmpty());
            assertTrue(log.contains("pat"), "processAnnotatedType must have fired");
            assertTrue(log.contains("atd"), "afterTypeDiscovery must have fired");
            assertEquals("atd", log.get(log.size() - 1),
                    "AfterTypeDiscovery must be the last ContainerListener callback");
        }
    }
}
