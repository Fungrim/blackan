package io.github.fungrim.blackan.injector;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

import io.github.fungrim.blackan.common.cdi.AfterBeanDiscovery;
import io.github.fungrim.blackan.common.cdi.AfterDeploymentValidation;
import io.github.fungrim.blackan.common.cdi.AfterTypeDiscovery;
import io.github.fungrim.blackan.common.cdi.BeforeBeanDiscovery;
import io.github.fungrim.blackan.common.cdi.BeforeShutdown;
import io.github.fungrim.blackan.common.cdi.ContainerExtension;
import io.github.fungrim.blackan.common.cdi.ContainerListener;
import io.github.fungrim.blackan.common.cdi.DefaultLifecycleEvent;
import io.github.fungrim.blackan.common.cdi.ObserverMethod;
import io.github.fungrim.blackan.common.cdi.ProcessAnnotatedType;
import io.github.fungrim.blackan.common.cdi.ProcessObserverMethod;
import io.github.fungrim.blackan.common.util.Arguments;
import io.github.fungrim.blackan.injector.context.ClassAccess;
import io.github.fungrim.blackan.injector.context.DecoratedInstance;
import io.github.fungrim.blackan.injector.context.ProcessScopeProvider;
import io.github.fungrim.blackan.injector.context.ScopeRegistry;
import io.github.fungrim.blackan.injector.context.ScopeRegistry.Execution;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.creator.DestroyableTracker;
import io.github.fungrim.blackan.injector.creator.ProviderFactory;
import io.github.fungrim.blackan.injector.creator.ScopeProviderFactory;
import io.github.fungrim.blackan.injector.lookup.CachingInstanceFactory;
import io.github.fungrim.blackan.injector.lookup.EventCoordinator;
import io.github.fungrim.blackan.injector.lookup.InjectionPointLookupKey;
import io.github.fungrim.blackan.injector.lookup.InstanceFactory;
import io.github.fungrim.blackan.injector.lookup.LimitedInstance;
import io.github.fungrim.blackan.injector.lookup.ObserverRegistry;
import io.github.fungrim.blackan.injector.producer.ProducerRegistry;
import io.github.fungrim.blackan.injector.util.SafeCallable;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.NotificationOptions;
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
    private final List<ContainerListener> containerListeners;
    private final Set<DotName> vetoedTypes;

    private Context(
            IndexView index,
            Context parent,
            Scope scope,
            ClassLoader classLoader,
            ProducerRegistry producerRegistry,
            Comparator<ClassInfo> eventOrdering,
            ExecutorService executorService,
            Object lifecycleEventPayload,
            ScopeRegistry scopeRegistry,
            List<ContainerListener> containerListeners
        ) {
        this.index = index;
        this.parent = parent;
        this.scope = scope;
        this.classLoader = classLoader;
        this.producerRegistry = producerRegistry;
        this.executorService = executorService;
        this.lifecycleEventPayload = lifecycleEventPayload != null ? lifecycleEventPayload : new DefaultLifecycleEvent();
        this.containerListeners = containerListeners != null ? List.copyOf(containerListeners) : List.of();
        if(eventOrdering == null) {
            this.eventOrdering = (a, b) -> 0;
        } else {
            this.eventOrdering = eventOrdering;
        }
        this.observerRegistry = new ObserverRegistry();
        this.scopeRegistry = scopeRegistry;
        Set<DotName> workingVetoedTypes = parent != null ? parent.vetoedTypes : new HashSet<>();
        this.creatorFactory = new ScopeProviderFactory(this);
        this.instanceFactory = new CachingInstanceFactory(creatorFactory, index, workingVetoedTypes);
        initialize(workingVetoedTypes);
        this.vetoedTypes = parent != null ? workingVetoedTypes : Collections.unmodifiableSet(workingVetoedTypes);
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
        private final List<ContainerListener> listeners = new ArrayList<>();

        public Builder withCustomEventOrdering(Comparator<ClassInfo> eventOrdering) {
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

        public Builder withListener(ContainerListener listener) {
            this.listeners.add(listener);
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
            DotName extName = DotName.createSimple(ContainerExtension.class);
            Set<String> addedExtensions = new HashSet<>();
            for (ContainerListener l : listeners) {
                addedExtensions.add(l.getClass().getName());
            }
            for (ClassInfo ci : index.getAllKnownImplementations(extName)) {
                if (ci.isInterface() || Modifier.isAbstract(ci.flags())) {
                    continue;
                }
                if (addedExtensions.contains(ci.name().toString())) {
                    continue;
                }
                try {
                    Class<?> cl = classLoader.loadClass(ci.name().toString());
                    ContainerExtension ext = (ContainerExtension) cl.getDeclaredConstructor().newInstance();
                    listeners.add(ext);
                    addedExtensions.add(ci.name().toString());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to instantiate ContainerExtension: " + ci.name(), e);
                }
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
                new ScopeRegistry(scopeProvider),
                listeners
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

    private void initialize(Set<DotName> mutableVetoedTypes) {
        if (parent == null) {
            BeforeBeanDiscovery bbd = new BeforeBeanDiscovery();
            for (ContainerListener listener : containerListeners) {
                listener.beforeBeanDiscovery(bbd);
            }
            for (ClassInfo classInfo : index.getKnownClasses()) {
                ProcessAnnotatedType event = new ProcessAnnotatedType(classInfo);
                for (ContainerListener listener : containerListeners) {
                    listener.processAnnotatedType(event);
                }
                if (event.isVetoed()) {
                    mutableVetoedTypes.add(classInfo.name());
                }
            }
            AfterTypeDiscovery atd = new AfterTypeDiscovery();
            for (ContainerListener listener : containerListeners) {
                listener.afterTypeDiscovery(atd);
            }
        }
        scanProducers();
        observerRegistry.scan(index);
        if (parent == null) {
            for (ObserverMethod om : observerRegistry.allObservers()) {
                ProcessObserverMethod event = new ProcessObserverMethod(om);
                for (ContainerListener listener : containerListeners) {
                    listener.processObserverMethod(event);
                }
                fireInternal(event);
                if (event.isVetoed()) {
                    observerRegistry.remove(om);
                }
            }
            AfterBeanDiscovery abd = new AfterBeanDiscovery();
            for (ContainerListener listener : containerListeners) {
                listener.afterBeanDiscovery(abd);
            }
            fireInternal(abd);
            for (AfterBeanDiscovery.SyntheticBean<?> bean : abd.beans()) {
                producerRegistry.registerSynthetic(bean);
            }
            AfterDeploymentValidation adv = new AfterDeploymentValidation();
            for (ContainerListener listener : containerListeners) {
                listener.afterDeploymentValidation(adv);
            }
            fireInternal(adv);
            if (!adv.getProblems().isEmpty()) {
                IllegalStateException ex = new IllegalStateException(
                        "Deployment validation failed with " + adv.getProblems().size() + " problem(s)");
                adv.getProblems().forEach(ex::addSuppressed);
                throw ex;
            }
        }
        fireLifecycleEvent(Initialized.Literal.of(scope.annotationClass()));
    }

    private void fireInternal(Object event) {
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(event, List.of()), event);
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
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(lifecycleEventPayload, jandexQualifiers), lifecycleEventPayload);
    }

    // --- Lifecycle ---

    @Override
    public void close() {
        if (!isClosing.compareAndSet(false, true)) {
            return;
        }
        if (parent == null) {
            BeforeShutdown bs = new BeforeShutdown();
            for (ContainerListener listener : containerListeners) {
                listener.beforeShutdown(bs);
            }
            fireInternal(bs);
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
        List<AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(event, jandexQualifiers), event);
    }

    public void fireInCustomOrder(Object event, Annotation... qualifiers) {
        checkClosed();
        List<AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(event, jandexQualifiers, eventOrdering), event);
    }

    public void fireInReverseCustomOrder(Object event, Annotation... qualifiers) {
        checkClosed();
        List<AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        EventCoordinator.fireObservers(this, observerRegistry.matchSync(event, jandexQualifiers, eventOrdering.reversed()), event);
    }

    public CompletionStage<Object> fireAsync(Object event, Annotation... qualifiers) {
        checkClosed();
        List<AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        return EventCoordinator.fireObserversAsync(this, executorService, observerRegistry.matchAsync(event, jandexQualifiers), event);
    }

    public CompletionStage<Object> fireAsync(Object event, NotificationOptions options, Annotation... qualifiers) {
        checkClosed();
        java.util.concurrent.Executor executor = (options != null && options.getExecutor() != null)
                ? options.getExecutor()
                : executorService;
        List<AnnotationInstance> jandexQualifiers = EventCoordinator.toJandexQualifiers(qualifiers);
        return EventCoordinator.fireObserversAsync(this, executor, observerRegistry.matchAsync(event, jandexQualifiers), event);
    }

    // --- Lookup ---

    /**
     * Lookup a class from the context. This does not return classes that
     * are not in the context, so a class might still be possible to load even
     * if not found by this method.  
     * 
     * @param name The name of the class, must not be null
     * @return An optional class access, never null
     */
    public Optional<ClassAccess> findClass(DotName name) {
        checkClosed();
        return Optional.ofNullable(index.getClassByName(name)).map(ClassAccess::of);
    }

    /**
     * Lookup a class from the context. This does not return classes that
     * are not in the context, so a class might still be possible to load even
     * if not found by this method.  
     * 
     * @param type The class to lookup, must not be null
     * @return An optional class access, never null
     */
    public Optional<ClassAccess> findClass(Class<?> type) {
        Arguments.notNull(type, "Type");
        return findClass(DotName.createSimple(type));
    }

    /**
     * Get an instance from the context. This does not return classes that
     * are not in the context, so a class might still be possible to load even
     * if not found by this method.  
     * 
     * @param name The name of the class, must not be null
     * @return An optional instance, never null
     */
    public LimitedInstance getInstance(DotName type) {
        checkClosed();
        Arguments.notNull(type, "Type");
        return instanceFactory.create(InjectionPointLookupKey.of(type));
    }

    /**
     * Get an instance from the context. This does not return classes that
     * are not in the context, so a class might still be possible to load even
     * if not found by this method.  
     * 
     * @param name The name of the class, must not be null
     * @return An optional instance, never null
     */
    public LimitedInstance getInstance(ClassInfo type) {
        checkClosed();
        Arguments.notNull(type, "Type");
        return instanceFactory.create(InjectionPointLookupKey.of(type));
    }

    /**
     * Get an instance from the context. This does not return classes that
     * are not in the context, so a class might still be possible to load even
     * if not found by this method.  
     * 
     * @param type The class to lookup, must not be null
     * @return An optional instance, never null
     */
    public LimitedInstance getInstance(Class<?> type) {
        Arguments.notNull(type, "Type");
        return getInstance(DotName.createSimple(type));
    }

    /**
     * Get an instance from the context. This does not return classes that
     * are not in the context, so a class might still be possible to load even
     * if not found by this method.  
     * 
     * @param type The class to lookup, must not be null
     * @return An optional instance, never null
     */
    public <T> T get(Class<T> type) {
        Arguments.notNull(type, "Type");
        return getInstance(type).get(type);
    }

    /**
     * Decorate an instance with interceptors and observers.
     * 
     * @param <T> The type of the instance
     * @param instance The instance to decorate, must not be null
     * @return A decorated instance, never null
     */
    public <T> DecoratedInstance<T> decorate(T instance) {
        checkClosed();
        Arguments.notNull(instance, "Instance");
        return instanceFactory.decorate(instance);
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

    /**
     * Load a class from the context classloader. This will load classes
     * that are not in the context as well. 
     * 
     * @param type The class to load, must not be null
     * @throws ConstructionException if the class cannot be loaded
     * @return The loaded class, never null
     */
    public Class<?> loadClass(Class<?> type) {
        Arguments.notNull(type, "Type");
        return loadClass(DotName.createSimple(type));
    }

    /**
     * Load a named class from the context classloader. This will load classes
     * that are not in the context as well. 
     * 
     * @param type The class to load, must not be null
     * @throws ConstructionException if the class cannot be loaded
     * @return The loaded class, never null
     */
    public Class<?> loadClass(DotName clazz) {
        Arguments.notNull(clazz, "Class");
        var opt = findClass(clazz);
        if (opt.isEmpty()) {
            // load from outside of index
            return loadClass(ClassAccess.of(clazz));
        } else {
            return loadClass(opt.get());
        }
    }

    private Class<?> loadClass(ClassAccess access) {
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

    public <T> T enterSafeScope(SafeCallable<T> supplier) {
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
            scopeRegistry,
            List.of());
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

}
