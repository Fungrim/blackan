# Blackan Injector

A lightweight CDI-inspired dependency injection container for Java. It uses [Jandex](https://github.com/smallrye/jandex) for annotation indexing and supports constructor, field, and method injection, scoped contexts, lifecycle callbacks, producer methods, and CDI events — all without runtime proxies.

## Creating a Context

Use `Context.builder()` to create an application-scoped root context. Provide either a list of classes or a pre-built Jandex index.

```java
// From a list of classes
Context context = Context.builder()
        .withClasses(List.of(MyService.class, MyRepository.class))
        .build();

// From a Jandex index
Context context = Context.builder()
        .withIndex(index)
        .build();

// Retrieve a bean
MyService service = context.get(MyService.class);
```

## Subcontexts

Subcontexts model narrower scopes such as session or request. They inherit beans from parent contexts and manage their own lifecycle.

```java
Context session = root.subcontext(Scope.SESSION);
Context request = session.subcontext(Scope.REQUEST);

// Beans scoped to @SessionScoped are created and cached in the session context.
// Closing the session destroys its beans and fires lifecycle events.
session.close();
```

A context can be entered, in which case it is associated with an internal thread local what
is used for nested context access: 

```java
context.enterScope(() -> {
    // Code here runs with this context as the current context
});
```

If the scope is not entered, container does not automatically associate subcontexts with threads or requests. In such a case, the caller is responsible for tracking the active context (e.g. in a `ThreadLocal` or request attribute) and making it available through a `ProcessScopeProvider`. This provider is consulted whenever the container needs to resolve which context is current for the running thread.

```java
AtomicReference<Context> current = new AtomicReference<>();

Context root = Context.builder()
        .withClasses(List.of(MyService.class))
        .withScopeProvider(() -> current.get())
        .build();
current.set(root);

// On a new request, push a request subcontext
Context request = root.subcontext(Scope.REQUEST);
current.set(request);

// When the request ends, close it and restore the parent
request.close();
current.set(root);
```

## Events

Fire events to `@Observes` and `@ObservesAsync` observer methods. Observer parameters beyond the event itself are injected by the container.

```java
// Synchronous
context.fire("some event");

// Asynchronous (returns CompletionStage)
context.fireAsync("async event");

// With qualifiers
context.fire(payload, new MyQualifierLiteral());
```

Observer methods:

```java
@ApplicationScoped
public class MyObserver {

    public void onEvent(@Observes String event) {
        // called synchronously
    }

    public void onAsyncEvent(@ObservesAsync String event, SomeService service) {
        // called asynchronously; SomeService is injected
    }
}
```

## Lifecycle Callbacks

`@PostConstruct` and `@PreDestroy` are supported on managed beans. `@Initialized`, `@BeforeDestroyed`, and `@Destroyed` lifecycle events are fired per-scope and their observer methods may also have injected parameters.

```java
@ApplicationScoped
public class StartupListener {

    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object event) { }

    public void onShutdown(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) { }
}
```

## No Client Proxies

Unlike a full CDI container, Blackan does **not** generate client proxies for normal-scoped beans. Beans are created eagerly when first looked up and injected as direct references. This means you cannot directly inject a shorter-lived bean into a longer-lived one — the reference would be captured at injection time and never refreshed.

Instead you should inject a `Provider<T>` that would resolve the correct instance on every call. For example, injecting a `@RequestScoped` bean into a `@SessionScoped` bean via a plain field would pin the first request's instance for the lifetime of the session. Instead, inject a `Provider<T>` so the container resolves the current instance on every call:

```java
@SessionScoped
public class SessionService {

    @Inject
    Provider<RequestInfo> requestInfoProvider;

    public void handleRequest() {
        // Resolved against the current request context each time
        RequestInfo info = requestInfoProvider.get();
    }
}
```

The same pattern applies whenever a wider scope needs access to a narrower scope — `@ApplicationScoped` to `@SessionScoped`, `@SessionScoped` to `@RequestScoped`, etc. `jakarta.enterprise.inject.Instance<T>` works the same way and additionally supports ambiguous/unsatisfied resolution checks.

## Container Lifecycle Events

The container fires a sequence of events during startup and shutdown. These allow you to observe and influence the bean discovery process.

### Full-lifecycle callbacks via `ContainerListener`

`ContainerListener` covers the complete container lifecycle — from initial type scanning through shutdown. Register one or more listeners on the builder via `withListener()` (called multiple times to add multiple listeners):

```java
Context context = Context.builder()
        .withClasses(List.of(MyService.class))
        .withListener(new ContainerListener() {

            @Override
            public void beforeBeanDiscovery(BeforeBeanDiscovery event) {
                // fires once, before any type is processed
            }

            @Override
            public void processAnnotatedType(ProcessAnnotatedType event) {
                // fires once per class in the index; call event.veto() to exclude
                if (shouldExclude(event.type())) {
                    event.veto();
                }
            }

            @Override
            public void afterTypeDiscovery(AfterTypeDiscovery event) {
                // fires after all ProcessAnnotatedType callbacks
            }

            @Override
            public void processObserverMethod(ProcessObserverMethod event) {
                // fires before the same event reaches @Observes methods
                if (shouldVetoObserver(event.observerMethod())) {
                    event.veto();
                }
            }

            @Override
            public void afterBeanDiscovery(AfterBeanDiscovery event) {
                // fires before the same event reaches @Observes methods
                // use addBean() to register programmatic beans
                event.addBean(MyService.class, ApplicationScoped.class, MyServiceImpl::new);
            }

            @Override
            public void afterDeploymentValidation(AfterDeploymentValidation event) {
                // report validation errors; a non-empty problem list aborts startup
                if (!configIsValid()) {
                    event.addDeploymentProblem(new IllegalStateException("Config missing"));
                }
            }

            @Override
            public void beforeShutdown(BeforeShutdown event) {
                // fires before the same event reaches @Observes methods
            }
        })
        .build();
```

For the pre-scan callbacks (`beforeBeanDiscovery`, `processAnnotatedType`, `afterTypeDiscovery`) observer scanning has not yet occurred, so they cannot be received via `@Observes`.

For post-scan callbacks (`processObserverMethod`, `afterBeanDiscovery`, `afterDeploymentValidation`, `beforeShutdown`) the `ContainerListener` is called **before** the same event is fired to `@Observes` methods in managed beans, so listeners always see the event first.

Vetoed types are invisible to the container: `getInstance()` returns an unsatisfied result and injected `Provider<T>` / `Instance<T>` will not resolve them.

### Synthetic beans via `AfterBeanDiscovery.addBean()`

Any `ContainerListener` can register programmatic beans during `afterBeanDiscovery`. Synthetic beans are backed by a `Supplier` and participate in the container as if they were `@Produces` methods:

```java
@Override
public void afterBeanDiscovery(AfterBeanDiscovery event) {
    // register a singleton backed by a supplier
    event.addBean(DataSource.class, ApplicationScoped.class, this::buildDataSource);

    // with qualifiers
    event.addBean(DataSource.class, ApplicationScoped.class, this::buildReadReplica,
            new NamedLiteral("readReplica"));
}
```

Synthetic beans without a scope annotation are treated as `@Dependent`. Scopes other than `@Dependent` are cached as singletons (no client proxies).

### Jandex-discoverable extensions via `ContainerExtension`

For reusable extensions that should be discovered automatically, implement `ContainerExtension` instead of `ContainerListener`. Any non-abstract class in the Jandex index that directly implements `ContainerExtension` is discovered and instantiated via its public no-arg constructor before any lifecycle callbacks fire:

```java
public class MyExtension implements ContainerExtension {

    @Override
    public void beforeBeanDiscovery(BeforeBeanDiscovery event) { ... }

    @Override
    public void afterBeanDiscovery(AfterBeanDiscovery event) {
        event.addBean(MyService.class, ApplicationScoped.class, MyServiceImpl::new);
    }
}
```

The extension class must be included in the Jandex index passed to the builder. If the same class is also registered programmatically via `withListener()`, only the programmatic instance is used.

`ContainerExtension` implementations are instantiated before the DI container is ready and do not support dependency injection during lifecycle callbacks.

### Post-scan events via `@Observes`

The lifecycle events that fire after scanning also reach `@Observes` methods on managed beans. `ContainerListener` and `ContainerExtension` callbacks always fire before these:

| Event | When | Notes |
|---|---|---|
| `ProcessObserverMethod` | Once per discovered observer method | Call `event.veto()` to remove the observer from the registry |
| `AfterBeanDiscovery` | After all observers and producers are scanned | Call `event.addBean()` to add synthetic beans |
| `AfterDeploymentValidation` | Immediately after `AfterBeanDiscovery` | Call `event.addDeploymentProblem()` to abort startup |
| `BeforeShutdown` | At the start of `Context.close()` | Fires only for the root context, before `@BeforeDestroyed` |

```java
@ApplicationScoped
public class LifecycleObserver {

    public void onAfterBeanDiscovery(@Observes AfterBeanDiscovery event) { }

    public void onAfterDeploymentValidation(@Observes AfterDeploymentValidation event) { }

    public void onProcessObserverMethod(@Observes ProcessObserverMethod event) {
        // veto any observer declared on a specific class
        if (event.observerMethod().method().declaringClass().name().toString()
                .equals(SomeBean.class.getName())) {
            event.veto();
        }
    }

    public void onBeforeShutdown(@Observes BeforeShutdown event) { }
}
```

### Deviations from CDI 2.0

- **`BeforeBeanDiscovery`, `ProcessAnnotatedType`, `AfterTypeDiscovery`** are delivered via `ContainerListener`/`ContainerExtension`, not `@Observes`, because observer scanning has not yet occurred at that point.
- **`ProcessAnnotatedType`** receives a Jandex `ClassInfo` rather than a CDI `AnnotatedType<X>`, as the container has no full reflection metadata at that stage.
- **`ProcessObserverMethod`** receives the internal `ObserverMethod` record rather than `jakarta.enterprise.inject.spi.ObserverMethod<T>`, because the container does not implement the full `BeanManager` SPI.
- **`AfterBeanDiscovery.addBean()`** accepts a `Supplier<T>` and a scope annotation class rather than a full `BeanConfigurator`. Beans registered this way act as direct references (no scope proxies).
- **`ContainerExtension`** is discovered via Jandex index rather than `ServiceLoader`, and requires a public no-arg constructor.
- **`ProcessBean`, `ProcessBeanAttributes`, `ProcessInjectionPoint`, `ProcessInjectionTarget`, `ProcessProducer`** are not supported.
- **`BeforeShutdown`** fires only when the root (`ApplicationScoped`) context is closed. Closing a subcontext does not trigger it.
- Lifecycle events for subcontexts (`AfterBeanDiscovery`, `AfterDeploymentValidation`, etc.) do not fire again when a subcontext is created — they are root-context-only events.

## CDI Compatibility

Blackan Injector implements a practical subset of the CDI specification. Because there are no client proxies, lazy initialization and scope-crossing behavior differs from a full CDI container (see above).

Unsupported features include:

- **Client proxies** for normal-scoped beans
- **Interceptors** and **decorators**
- **Stereotypes**
- **Bean discovery modes** (`annotated`, `all`, `none`)
- **`@Disposes`** methods for producer cleanup
- **Full portable extensions** (`jakarta.enterprise.inject.spi.Extension` SPI) — limited support is available via `ContainerExtension` and `ContainerListener` (see above)
- **Build-compatible extensions** (Build Compatible Extensions SPI)
- **`ProcessBean`, `ProcessBeanAttributes`, `ProcessInjectionPoint`, `ProcessProducer`** lifecycle SPI events
- **Conversation scope** (`@ConversationScoped`)
- **`@Specializes`**
- **`Event<T>`** injectable wrapper (use `context.fire()` directly instead)
- **`BeanManager`** / `CDI.current()` programmatic lookup API
- **Bean Validation** integration
