package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.util.stubs.AppGreeting;
import io.github.fungrim.blackan.injector.util.stubs.Greeting;
import io.github.fungrim.blackan.injector.util.stubs.PostConstructBean;
import io.github.fungrim.blackan.injector.util.stubs.SessionLifecycleBean;

class DestroyTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();
    private Context root;

    @BeforeEach
    void setup() throws IOException {
        root = Context.builder()
                .withClasses(List.of(
                        PostConstructBean.class,
                        SessionLifecycleBean.class,
                        Greeting.class,
                        AppGreeting.class))
                .withScopeProvider(() -> currentContext.get())
                .build();
        currentContext.set(root);
    }

    @AfterEach
    void teardown() {
        root.close();
    }

    @Nested
    class DestroyByClass {

        @Test
        void invokesPreDestroyOnBean() {
            PostConstructBean bean = root.get(PostConstructBean.class);
            assertTrue(bean.postConstructCalled);
            assertFalse(bean.preDestroyCalled);

            root.destroy(PostConstructBean.class);
            assertTrue(bean.preDestroyCalled);
        }

        @Test
        void clearsCache_subsequentGetReturnsNewInstance() {
            PostConstructBean first = root.get(PostConstructBean.class);
            root.destroy(PostConstructBean.class);

            PostConstructBean second = root.get(PostConstructBean.class);
            assertNotSame(first, second);
            assertTrue(second.postConstructCalled);
        }

        @Test
        void destroyUnloadedBeanFinishesSilently() {
            root.destroy(PostConstructBean.class);
        }
    }

    @Nested
    class DestroyByDotName {

        @Test
        void invokesPreDestroyOnBean() {
            PostConstructBean bean = root.get(PostConstructBean.class);
            assertFalse(bean.preDestroyCalled);

            root.destroy(org.jboss.jandex.DotName.createSimple(PostConstructBean.class));
            assertTrue(bean.preDestroyCalled);
        }

        @Test
        void clearsCache_subsequentGetReturnsNewInstance() {
            PostConstructBean first = root.get(PostConstructBean.class);

            root.destroy(org.jboss.jandex.DotName.createSimple(PostConstructBean.class));

            PostConstructBean second = root.get(PostConstructBean.class);
            assertNotSame(first, second);
        }
    }

    @Nested
    class DestroyByClassInfo {

        @Test
        void invokesPreDestroyOnBean() {
            PostConstructBean bean = root.get(PostConstructBean.class);
            assertFalse(bean.preDestroyCalled);

            root.destroy(root.index().getClassByName(PostConstructBean.class));
            assertTrue(bean.preDestroyCalled);
        }
    }

    @Nested
    class DestroyInSubcontext {

        @Test
        void destroyInSessionContextDoesNotAffectRoot() {
            Context session = root.subcontext(Scope.SESSION);
            currentContext.set(session);

            SessionLifecycleBean sessionBean = session.get(SessionLifecycleBean.class);
            assertTrue(sessionBean.postConstructCalled);
            assertFalse(sessionBean.preDestroyCalled);

            session.destroy(SessionLifecycleBean.class);
            assertTrue(sessionBean.preDestroyCalled);

            session.close();
        }
    }
}
