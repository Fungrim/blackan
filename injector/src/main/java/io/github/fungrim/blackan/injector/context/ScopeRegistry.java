package io.github.fungrim.blackan.injector.context;

import java.io.Closeable;
import java.util.Optional;
import java.util.Stack;

import io.github.fungrim.blackan.injector.Context;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ScopeRegistry {

    private final ThreadLocal<Stack<Context>> currentContext = ThreadLocal.withInitial(() -> new Stack<Context>());

    @FunctionalInterface
    public static interface Execution extends Closeable {

        public void close();
    
    }

    private final ProcessScopeProvider scopeProvider;

    public Execution enter(Context context) {
        currentContext.get().push(scopeProvider == null ? context : scopeProvider.current());
        return () -> currentContext.get().pop();
    }

    public Optional<Context> current() {
        Stack<Context> stack = currentContext.get();
        return stack.isEmpty() ? Optional.ofNullable(scopeProvider == null ? null : scopeProvider.current()) : Optional.of(stack.peek());
    }
}
