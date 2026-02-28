package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.util.stubs.AppGreeting;
import io.github.fungrim.blackan.injector.util.stubs.Greeting;

class EnterScopeTest {

    private Context root;

    @BeforeEach
    void setup() throws IOException {
        root = Context.builder()
                .withClasses(List.of(Greeting.class, AppGreeting.class))
                .build();
    }

    @AfterEach
    void teardown() {
        root.close();
    }

    @Nested
    class RunnableOverload {

        @Test
        void executesRunnableBody() {
            AtomicReference<Boolean> executed = new AtomicReference<>(false);
            root.enterScope(() -> executed.set(true));
            assertTrue(executed.get());
        }

        @Test
        void setsCurrentScopeDuringExecution() {
            AtomicReference<Optional<Context>> captured = new AtomicReference<>();
            root.enterScope(() -> captured.set(root.currentScope()));
            assertSame(root, captured.get().orElse(null));
        }

        @Test
        void clearsCurrentScopeAfterExecution() {
            root.enterScope(() -> {});
            assertTrue(root.currentScope().isEmpty());
        }

        @Test
        void clearsCurrentScopeAfterException() {
            assertThrows(RuntimeException.class, () ->
                    root.enterScope(() -> { throw new RuntimeException("boom"); }));
            assertTrue(root.currentScope().isEmpty());
        }

        @Test
        void propagatesRuntimeException() {
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    root.enterScope(() -> { throw new RuntimeException("boom"); }));
            assertEquals("boom", ex.getMessage());
        }
    }

    @Nested
    class CallableOverload {

        @Test
        void returnsCallableResult() throws Exception {
            String result = root.enterScope(() -> "hello");
            assertEquals("hello", result);
        }

        @Test
        void setsCurrentScopeDuringExecution() throws Exception {
            Context captured = root.enterScope(() -> root.currentScope().orElse(null));
            assertSame(root, captured);
        }

        @Test
        void clearsCurrentScopeAfterExecution() throws Exception {
            root.enterScope(() -> "done");
            assertTrue(root.currentScope().isEmpty());
        }

        @Test
        void propagatesCheckedException() {
            assertThrows(IOException.class, () ->
                    root.enterScope(() -> { throw new IOException("io fail"); }));
            assertTrue(root.currentScope().isEmpty());
        }

        @Test
        void propagatesRuntimeException() {
            assertThrows(RuntimeException.class, () ->
                    root.enterScope(() -> { throw new RuntimeException("boom"); }));
            assertTrue(root.currentScope().isEmpty());
        }
    }

    @Nested
    class Nesting {

        @Test
        void nestedEnterScopeUsesInnermostContext() throws Exception {
            Context session = root.subcontext(Scope.SESSION);

            AtomicReference<Context> innerCaptured = new AtomicReference<>();
            AtomicReference<Context> outerAfterInner = new AtomicReference<>();

            root.enterScope(() -> {
                session.enterScope(() -> {
                    innerCaptured.set(root.currentScope().orElse(null));
                });
                outerAfterInner.set(root.currentScope().orElse(null));
            });

            assertSame(session, innerCaptured.get());
            assertSame(root, outerAfterInner.get());

            session.close();
        }

        @Test
        void nestedScopeIsRestoredAfterInnerException() {
            Context session = root.subcontext(Scope.SESSION);

            root.enterScope((Runnable) () -> {
                try {
                    session.enterScope((Runnable) () -> { throw new RuntimeException("inner"); });
                } catch (RuntimeException ignored) {}

                assertSame(root, root.currentScope().orElse(null));
            });

            session.close();
        }
    }
}
