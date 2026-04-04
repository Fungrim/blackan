package io.github.fungrim.blackan.extension.jetty;

import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.Callable;

import io.github.fungrim.blackan.injector.Context;
import jakarta.servlet.ServletException;

public class ServletScopedContext {

    private static final ThreadLocal<Stack<Context>> currentContext = ThreadLocal.withInitial(() -> new Stack<Context>());

    public static void enter(Context context, ServletFilterExecution exec) throws ServletException {
        currentContext.get().push(context);
        try {
            context.enterScope(exec::run);
        } catch(ServletRuntimeException e) {
            throw new ServletException(e);
        } finally {
            currentContext.get().pop();
        }
    }

    public static <T> T enter(Context context, Callable<T> callable) throws Exception {
        currentContext.get().push(context);
        try {
            return context.enterScope(callable);
        } finally {
            currentContext.get().pop();
        }
    }

    public static Optional<Context> current() {
        Stack<Context> stack = currentContext.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
    }
}
