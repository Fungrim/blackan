package io.github.fungrim.blackan.injector.creator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Provider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ScopeProviderFactory implements ProviderFactory {

    private final Context context;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<ClassInfo, Provider<?>> cache = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Provider<T> create(ClassInfo type, Optional<InjectionLocation<T>> location, Class<T> clazzType) {
        lock.writeLock().lock();
        try {
            if(cache.containsKey(type)) {
                return (Provider<T>) cache.get(type);
            } else {
                Scope scope = Scope.of(type).orElse(Scope.DEPENDENT);
                if(!isInjectionLegal(scope, type)) {
                    throw new ConstructionException("Injection of type " + type + " is not legal, cannot inject object of scope " + scope + " into context with scope " + context.scope() + ", consider injecting a Provider instead");
                }
                // delegate to root if not already there
                if((scope == Scope.APPLICATION || scope == Scope.SINGLETON) && context.parent().isPresent()) {
                    return context.root().getInstance(type).toProvider(clazzType);
                }
                // yield if required
                if(scope.shouldYield(context.scope()) && context.parent().isPresent()) {
                    return context.parent().get().getInstance(type).toProvider(clazzType);
                }
                // create provider using the concrete class from the ClassInfo
                Class<T> concreteClass = (Class<T>) context.loadClass(type.name());
                DependentProvider<T> provider = new DependentProvider<>(context, type, concreteClass);
                if(scope == Scope.DEPENDENT) {
                    return (Provider<T>) cache.computeIfAbsent(type, t -> provider);
                } else {
                    return (Provider<T>) cache.computeIfAbsent(type, t -> new SingletonProvider<>(provider));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        cache.clear();
    }

    private boolean isInjectionLegal(Scope scope, ClassInfo type) {
        if(scope == Scope.DEPENDENT || scope.priority() == context.scope().priority()) {
            // you can always inject a dependent or same scope 
            return true;
        }
        if(scope == Scope.SINGLETON) {
            // you can always inject a singleton, it will load in the application scope
            return true;
        }
        if(scope.shouldYield(context.scope())) {
            // it's ok to inject a lower scope into a higher scope
            return true;
        } else {
            // this is a reverse injection, only allow Instance and Provider injection
            return isInstanceOrProvider(type);
        }
    }

    private boolean isInstanceOrProvider(ClassInfo type) {
        return type.name().equals(DotName.createSimple(Instance.class)) || type.name().equals(DotName.createSimple(Provider.class));
    }
}
