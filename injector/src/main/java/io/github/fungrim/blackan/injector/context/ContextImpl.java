package io.github.fungrim.blackan.injector.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import io.github.fungrim.blackan.common.Arguments;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import io.github.fungrim.blackan.injector.creator.ScopeProviderFactory;
import io.github.fungrim.blackan.injector.lookup.CachingInstanceFactory;
import io.github.fungrim.blackan.injector.lookup.InstanceFactory;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;

public class ContextImpl implements Context {

    protected final Index index;
    protected final Context parent;
    protected final ProviderFactory creatorFactory;
    protected final InstanceFactory instanceFactory;
    protected final Scope scope;
    protected final ClassLoader classLoader;
    protected final ProcessScopeProvider scopeProvider;

    public ContextImpl(Index index, Context parent, Scope scope, ClassLoader classLoader, ProcessScopeProvider scopeProvider) {
        this.index = index;
        this.parent = parent;
        this.scope = scope;
        this.classLoader = classLoader;
        if(scopeProvider == null) {
            this.scopeProvider = () -> this;
        } else {
            this.scopeProvider = scopeProvider;
        }
        this.creatorFactory = new ScopeProviderFactory(this);
        this.instanceFactory = new CachingInstanceFactory(creatorFactory);
    }
    
    @Override
    public Index index() {
        return index;
    }

    @Override
    public Optional<Context> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Scope scope() {
        return scope;
    }

    @Override
    public ClassLoader classLoader() {
        return classLoader;
    }

    @Override
    public ProcessScopeProvider processScopeProvider() {
        return scopeProvider;
    }

    @Override
    public LimitedInstance getInstance(DotName type) {
        Arguments.notNull(type, "Type");
        RecursionKey key = RecursionKey.of(type);
        ClassAccess access = findClass(type).orElse(ClassAccess.of(loadClassOutsideOfIndex(type)));
        if(access.isInterface()) {
            return instanceFactory.create(key, index.getAllKnownImplementations(key.type()));
        } else {
            return instanceFactory.create(key, includeSelf(key.type()));
        }
    }

    @Override
    public LimitedInstance getInstance(ClassInfo type) {
        Arguments.notNull(type, "Type");
        RecursionKey key = RecursionKey.of(type);
        if(type.isInterface()) {
            return instanceFactory.create(key, index.getAllKnownImplementations(key.type()));
        } else {
            return instanceFactory.create(key, includeSelf(key.type()));
        }
    }

    private Collection<ClassInfo> includeSelf(DotName type) {
        List<ClassInfo> candidates = new ArrayList<>(index.getAllKnownSubclasses(type));
        ClassInfo self = index.getClassByName(type);
        if(self != null) {
            candidates.add(0, self);
        }
        return candidates;
    }

    private Class<?> loadClassOutsideOfIndex(DotName type) {
        try {
            return classLoader().loadClass(type.toString());
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Failed to load class: " + type.toString(), e);
        }
    }

    @Override
    public Optional<ClassAccess> findClass(DotName name) {
        return Optional.ofNullable(index.getClassByName(name)).map(ClassAccess::of);
    }
}
