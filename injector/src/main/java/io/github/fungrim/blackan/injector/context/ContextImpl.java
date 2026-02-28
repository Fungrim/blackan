package io.github.fungrim.blackan.injector.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.github.fungrim.blackan.common.util.Arguments;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.Scope;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import io.github.fungrim.blackan.injector.creator.ScopeProviderFactory;
import io.github.fungrim.blackan.injector.event.EventCoordinator;
import io.github.fungrim.blackan.injector.lookup.CachingInstanceFactory;
import io.github.fungrim.blackan.injector.lookup.InstanceFactory;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;
import io.github.fungrim.blackan.injector.producer.ProducerRegistry;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.inject.Produces;

public class ContextImpl implements Context {

    private static final DotName PRODUCES = DotName.createSimple(Produces.class);
    private static final DotName INITIALIZED = DotName.createSimple(Initialized.class);
    private static final DotName BEFORE_DESTROYED = DotName.createSimple(BeforeDestroyed.class);
    private static final DotName DESTROYED = DotName.createSimple(Destroyed.class);

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    protected final IndexView index;
    protected final Context parent;
    protected final ProviderFactory creatorFactory;
    protected final InstanceFactory instanceFactory;
    protected final Scope scope;
    protected final ClassLoader classLoader;
    protected final ProcessScopeProvider scopeProvider;
    protected final ProducerRegistry producerRegistry;
    protected final Comparator<ClassInfo> eventOrdering;

    private List<MethodInfo> initializedMethods;
    private List<MethodInfo> beforeDestroyedMethods;
    private List<MethodInfo> destroyedMethods;

    public ContextImpl(
            IndexView index, 
            Context parent, 
            Scope scope, 
            ClassLoader classLoader, 
            ProcessScopeProvider scopeProvider, 
            ProducerRegistry producerRegistry, 
            Comparator<ClassInfo> eventOrdering
        ) {
        this.index = index;
        this.parent = parent;
        this.scope = scope;
        this.classLoader = classLoader;
        this.producerRegistry = producerRegistry;
        if(scopeProvider == null) {
            this.scopeProvider = () -> this;
        } else {
            this.scopeProvider = scopeProvider;
        }
        this.creatorFactory = new ScopeProviderFactory(this);
        this.instanceFactory = new CachingInstanceFactory(creatorFactory);
        if(eventOrdering == null) {
            this.eventOrdering = (a, b) -> 0;
        } else {
            this.eventOrdering = eventOrdering;
        }
        initialize();
    }

    protected void initialize() {
        scanProducers();
        initializedMethods = scanEvents(INITIALIZED);
        beforeDestroyedMethods = scanEvents(BEFORE_DESTROYED);
        destroyedMethods = scanEvents(DESTROYED);
        EventCoordinator.fire(this, initializedMethods);
    }

    protected void scanProducers() {
        for (AnnotationInstance annotation : index.getAnnotations(PRODUCES)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                producerRegistry.register(this, annotation.target().asMethod());
            }
        }
    }

    protected List<MethodInfo> scanEvents(DotName annotation) {
        List<MethodInfo> methods = new ArrayList<>();
        for (AnnotationInstance instance : index.getAnnotations(annotation)) {
            if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
                methods.add(instance.target().asMethod());
            }
        }
        methods.sort(Comparator.comparing(MethodInfo::declaringClass, eventOrdering));
        return methods;
    }

    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    @Override
    public void close() {
        if (!isClosing.compareAndSet(false, true)) {
            return;
        }
        EventCoordinator.fire(this, beforeDestroyedMethods);
        EventCoordinator.fire(this, destroyedMethods);
        isClosed.set(true);
        producerRegistry.close();
        instanceFactory.close();
        creatorFactory.close();
    }

    private void checkClosed() {
        if (isClosed.get()) {
            throw new IllegalStateException("Context is closed");
        }
    }
    
    @Override
    public IndexView index() {
        checkClosed();
        return index;
    }

    @Override
    public Optional<Context> parent() {
        checkClosed();
        return Optional.ofNullable(parent);
    }

    @Override
    public Comparator<ClassInfo> eventOrdering() {
        checkClosed();
        return eventOrdering;
    }

    @Override
    public Scope scope() {
        checkClosed();
        return scope;
    }

    @Override
    public ClassLoader classLoader() {
        checkClosed();
        return classLoader;
    }

    @Override
    public ProcessScopeProvider processScopeProvider() {
        checkClosed();
        return scopeProvider;
    }

    @Override
    public ProducerRegistry producerRegistry() {
        checkClosed();
        return producerRegistry;
    }

    @Override
    public LimitedInstance getInstance(DotName type) {
        checkClosed();
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
        checkClosed();
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
        checkClosed();
        return Optional.ofNullable(index.getClassByName(name)).map(ClassAccess::of);
    }
}
