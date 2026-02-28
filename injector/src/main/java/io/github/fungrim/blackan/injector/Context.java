package io.github.fungrim.blackan.injector;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.github.fungrim.blackan.common.cdi.DefaultLifecycleEvent;
import io.github.fungrim.blackan.common.util.Arguments;
import io.github.fungrim.blackan.injector.context.ClassAccessImpl;
import io.github.fungrim.blackan.injector.context.ClassInfoAccessImpl;
import io.github.fungrim.blackan.injector.context.ProcessScopeProvider;
import io.github.fungrim.blackan.injector.context.ScopeRegistry;
import io.github.fungrim.blackan.injector.context.ScopeRegistry.Execution;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.creator.DestroyableTracker;
import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import io.github.fungrim.blackan.injector.creator.ScopeProviderFactory;
import io.github.fungrim.blackan.injector.event.EventCoordinator;
import io.github.fungrim.blackan.injector.event.ObserverRegistry;
import io.github.fungrim.blackan.injector.lookup.CachingInstanceFactory;
import io.github.fungrim.blackan.injector.lookup.InstanceFactory;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;
import io.github.fungrim.blackan.injector.producer.ProducerRegistry;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.inject.Produces;

public class Context implements Closeable {

    private static final DotName PRODUCES = DotName.createSimple(Produces.class);

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    private final IndexView index;
    private final Context parent;
    private final ProviderFactory creatorFactory;
    private final InstanceFactory instanceFactory;
    private final Scope scope;
    private final ClassLoader classLoader;
    private final ProducerRegistry producerRegistry;
    private final Comparator<ClassInfo> eventOrdering;
    private final ExecutorService executorService;
    private final ObserverRegistry observerRegistry;
    private final Object lifecycleEventPayload;
    private final DestroyableTracker destroyableTracker = new DestroyableTracker();
    private final ScopeRegistry scopeRegistry;

    private Context(
            IndexView index, 
            Context parent, 
            Scope scope, 
            ClassLoader classLoader,  
            ProducerRegistry producerRegistry, 
            Comparator<ClassInfo> eventOrdering,
            ExecutorService executorService,
            Object lifecycleEventPayload,
            ScopeRegistry scopeRegistry
        ) {
        this.index = index;
        this.parent = parent;
        this.scope = scope;
        this.classLoader = classLoader;
        this.producerRegistry = producerRegistry;
        this.executorService = executorService;
        this.lifecycleEventPayload = lifecycleEventPayload != null ? lifecycleEventPayload : new DefaultLifecycleEvent();
        this.creatorFactory = new ScopeProviderFactory(this);
        this.instanceFactory = new CachingInstanceFactory(creatorFactory);
        if(eventOrdering == null) {
            this.eventOrdering = (a, b) -> 0;
        } else {
            this.eventOrdering = eventOrdering;
        }
        this.observerRegistry = new ObserverRegistry();
        this.scopeRegistry = scopeRegistry;
        initialize();
    }

    // --- ClassAccess interface ---

    public static interface ClassAccess {

        public static ClassAccess of(final Class<?> cl) {
            return new ClassAccessImpl(cl);
        }

        public static ClassAccess of(final ClassInfo info) {
            return new ClassInfoAccessImpl(info);
        }

        boolean isInterface();
    
        Class<?> load(ClassLoader loader);

        DotName name();

    }

    // --- Builder ---

    public static class Builder {

        private IndexView index;
        private List<Class<?>> classes;
        private ClassLoader classLoader;
        private ProcessScopeProvider scopeProvider;
        private Comparator<ClassInfo> eventOrdering;
        private ExecutorService executorService;
        private Object lifecycleEventPayload;

        public Builder withEventOrdering(Comparator<ClassInfo> eventOrdering) {
            this.eventOrdering = eventOrdering;
            return this;
        }

        public Builder withScopeProvider(ProcessScopeProvider scopeProvider) {
            this.scopeProvider = scopeProvider;
            return this;
        }

        public Builder withClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder withClasses(List<Class<?>> classes) {
            this.classes = classes;
            return this;
        }

        public Builder withIndex(IndexView index) {
            this.index = index;
            return this;
        }

        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder withLifecycleEventPayload(Object lifecycleEventPayload) {
            this.lifecycleEventPayload = lifecycleEventPayload;
            return this;
        }
     
        public Context build() throws IOException {
            if(index != null && classes != null) {
                throw new IllegalArgumentException("Index and classes cannot both be specified");
            }
            if(index == null && classes == null) {
                throw new IllegalArgumentException("Index or classes must be specified");
            }
            if(classes != null) {
                Indexer indexer = new Indexer();
                for (Class<?> cl : classes) {
                    indexer.indexClass(cl);
                }
                index = indexer.complete();
            }
            if(classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
                if(classLoader == null) {
                    classLoader = getClass().getClassLoader();
                }
            }
            if(eventOrdering == null) {
                eventOrdering = (a, b) -> 0;
            }
            if(executorService == null) {
                executorService = Executors.newCachedThreadPool(r -> {
                    Thread t = new Thread(r, "blackan-observer");
                    t.setDaemon(true);
                    return t;
                });
            }
            return new Context(
                index, 
                null, 
                Scope.APPLICATION, 
                classLoader, 
                new ProducerRegistry(), 
                eventOrdering, 
                executorService, 
                lifecycleEventPayload, 
                new ScopeRegistry(scopeProvider)
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Context of(Index index) {
        Arguments.notNull(index, "Index");
        try {
            return Context.builder().withIndex(index).build();
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- Initialization ---

    private void initialize() {
        scanProducers();
        observerRegistry.scan(index);
        fireLifecycleEvent(Initialized.Literal.of(scope.annotationClass()));
    }

    private void scanProducers() {
        for (AnnotationInstance annotation : index.getAnnotations(PRODUCES)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                producerRegistry.register(this, annotation.target().asMethod());
            }
        }
    }

    private void fireLifecycleEvent(Annotation qualifier) {
        List<AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(new Annotation[]{qualifier});
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(lifecycleEventPayload, jandexQualifiers, eventOrdering), lifecycleEventPayload);
    }

    // --- Lifecycle ---

    @Override
    public void close() {
        if (!isClosing.compareAndSet(false, true)) {
            return;
        }
        destroyableTracker.destroyAll();
        fireLifecycleEvent(BeforeDestroyed.Literal.of(scope.annotationClass()));
        fireLifecycleEvent(Destroyed.Literal.of(scope.annotationClass()));
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

    // --- Accessors ---

    public IndexView index() {
        checkClosed();
        return index;
    }

    public Optional<Context> parent() {
        checkClosed();
        return Optional.ofNullable(parent);
    }

    public Comparator<ClassInfo> eventOrdering() {
        checkClosed();
        return eventOrdering;
    }

    public Scope scope() {
        checkClosed();
        return scope;
    }

    public ClassLoader classLoader() {
        checkClosed();
        return classLoader;
    }
    
    public ProducerRegistry producerRegistry() {
        checkClosed();
        return producerRegistry;
    }

    public DestroyableTracker destroyableTracker() {
        return destroyableTracker;
    }

    public ExecutorService executorService() {
        return executorService;
    }

    public Optional<Context> currentScope() {
        return scopeRegistry.current();
    }

    public ScopeRegistry scopeRegistry() {
        return scopeRegistry;
    }

    // --- Events ---

    public void fire(Object event, Annotation... qualifiers) {
        checkClosed();
        List<org.jboss.jandex.AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(event, jandexQualifiers, (a, b) -> 0), event);
    }

    public void fireInOrder(Object event, Annotation... qualifiers) {
        checkClosed();
        List<org.jboss.jandex.AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(event, jandexQualifiers, eventOrdering), event);
    }

    public void fireInReverseOrder(Object event, Annotation... qualifiers) {
        checkClosed();
        List<org.jboss.jandex.AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(event, jandexQualifiers, eventOrdering.reversed()), event);
    }

    public CompletionStage<Object> fireAsync(Object event, Annotation... qualifiers) {
        checkClosed();
        List<org.jboss.jandex.AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        return EventCoordinator.fireObserversAsync(this, executorService, observerRegistry.matchAsync(event, jandexQualifiers, eventOrdering), event);
    }

    // --- Lookup ---

    public Optional<ClassAccess> findClass(DotName name) {
        checkClosed();
        return Optional.ofNullable(index.getClassByName(name)).map(ClassAccess::of);
    }

    public Optional<ClassAccess> findClass(Class<?> type) {
        Arguments.notNull(type, "Type");
        return findClass(DotName.createSimple(type));
    }

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

    public LimitedInstance getInstance(Class<?> type) {
        Arguments.notNull(type, "Type");
        return getInstance(DotName.createSimple(type));
    }

    public <T> T get(Class<T> type) {
        Arguments.notNull(type, "Type");
        return getInstance(type).get(type);
    }

    // --- Destroy ---

    public void destroy(DotName type) {
        Arguments.notNull(type, "Type");
        destroyableTracker.destroyByType(type, classLoader);
        creatorFactory.evict(type);
        instanceFactory.evict(type);
    }

    public void destroy(ClassInfo type) {
        Arguments.notNull(type, "Type");
        destroy(type.name());
    }

    public void destroy(Class<?> type) {
        Arguments.notNull(type, "Type");
        destroy(DotName.createSimple(type));
    }

    // --- Class loading ---

    public Class<?> loadClass(Class<?> type) {
        Arguments.notNull(type, "Type");
        return loadClass(DotName.createSimple(type));
    }

    public Class<?> loadClass(DotName clazz) {
        Arguments.notNull(clazz, "Class");
        return loadClass(findClass(clazz).orElseThrow(() -> new ConstructionException("Failed to find class: " + clazz.toString())));
    }

    public Class<?> loadClass(ClassAccess access) {
        Arguments.notNull(access, "Access");
        return access.load(classLoader());
    }

    // --- Scoped Execution ---

    public void enterScope(Runnable runnable) {
        try (Execution e = scopeRegistry.enter(this)) {
            runnable.run();
        }
    }

    public <T> T enterScope(Callable<T> supplier) throws Exception {
        try (Execution e = scopeRegistry.enter(this)) {
            return supplier.call();
        }
    }


    // --- Subcontext ---

    public Context subcontext(Scope scope) {
        return subcontext(scope, classLoader(), Optional.empty());
    }

    public Context subcontext(Scope scope, ClassLoader classLoader) {
        return subcontext(scope, classLoader, Optional.empty());
    }

    public Context subcontext(Scope scope, Optional<Object> lifecycleEventPayload) {
        return subcontext(scope, classLoader(), lifecycleEventPayload);
    }

    public Context subcontext(Scope scope, ClassLoader classLoader, Optional<Object> lifecycleEventPayload) {
        Arguments.notNull(scope, "Scope");
        Arguments.notNull(classLoader, "ClassLoader");
        Arguments.notNull(lifecycleEventPayload, "LifecycleEventPayload");
        return new Context(
            index(), 
            this, 
            scope, 
            classLoader, 
            producerRegistry(), 
            eventOrdering(), 
            executorService(), 
            lifecycleEventPayload.orElse(null), 
            scopeRegistry);
    }

    // --- Navigation ---

    public Context root() {
        Optional<Context> p = parent();
        if(p.isEmpty()) {
            return this;
        } else {
            Context root = p.get();
            while(root.parent().isPresent()) {
                root = root.parent().get();
            }
            return root;
        }
    }

    // --- Private helpers ---

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
}
