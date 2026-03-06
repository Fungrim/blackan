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

### Pre-scan callbacks via `ContainerListener`

The `BeforeBeanDiscovery`, `ProcessAnnotatedType`, and `AfterTypeDiscovery` events fire before any observer methods are registered, so they cannot be received via `@Observes`. Register a `ContainerListener` on the builder instead:

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
        })
        .build();
```

Vetoed types are invisible to the container: `getInstance()` returns an unsatisfied result and injected `Provider<T>` / `Instance<T>` will not resolve them.

### Post-scan events via `@Observes`

The following events fire after observer and producer scanning completes. They are delivered to `@Observes` methods in the normal way:

| Event | When | Notes |
|---|---|---|
| `ProcessObserverMethod` | Once per discovered observer method | Call `event.veto()` to remove the observer from the registry |
| `AfterBeanDiscovery` | After all observers and producers are scanned | |
| `AfterDeploymentValidation` | Immediately after `AfterBeanDiscovery` | Last event before the container becomes operational |
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

- **`BeforeBeanDiscovery`, `ProcessAnnotatedType`, `AfterTypeDiscovery`** are delivered via `ContainerListener`, not `@Observes`, because observer scanning has not yet occurred at that point. The CDI spec fires them to portable extensions instead.
- **`ProcessAnnotatedType`** receives a Jandex `ClassInfo` rather than a CDI `AnnotatedType<X>`, as the container has no full reflection metadata at that stage.
- **`ProcessObserverMethod`** receives the internal `ObserverMethod` record rather than `jakarta.enterprise.inject.spi.ObserverMethod<T>`, because the container does not implement the full `BeanManager` SPI.
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
- **Portable extensions** (`Extension` SPI)
- **Build-compatible extensions** (Build Compatible Extensions SPI)
- **`ProcessBean`, `ProcessBeanAttributes`, `ProcessInjectionPoint`, `ProcessProducer`** lifecycle SPI events
- **Conversation scope** (`@ConversationScoped`)
- **`@Specializes`**
- **`Event<T>`** injectable wrapper (use `context.fire()` directly instead)
- **`BeanManager`** / `CDI.current()` programmatic lookup API
- **Bean Validation** integration
