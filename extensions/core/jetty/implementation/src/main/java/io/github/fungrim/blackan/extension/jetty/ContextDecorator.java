package io.github.fungrim.blackan.extension.jetty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.Decorator;
import org.slf4j.Logger;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.context.DecoratedInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ContextDecorator implements Decorator {

    @Inject
    Context rootContext;

    @Inject
    Logger log;

    private final Map<Object, DecoratedInstance<?>> decoratedInstances = new HashMap<>();

    @Override
    public <T> T decorate(T o) {
        var opt = ServletScopedContext.current();
        var context = opt.orElse(rootContext);
        log.debug("Decorating {} with scope {}", o.getClass().getName(), context.scope());
        return context.enterSafeScope(() -> {
            var decoration = context.decorate(o);
            decoratedInstances.put(o, decoration);
            return decoration.get();
        });
    }

    @Override
    public void destroy(Object o) {
        var decoration = decoratedInstances.remove(o);
        if (decoration != null) {
            decoration.destroy();
        }
    }
}
