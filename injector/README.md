# Blackan Injector

A lightweight CDI-inspired dependency injection container for Java. It uses [Jandex](https://github.com/smallrye/jandex) for annotation indexing and supports constructor, field, and method injection, scoped contexts, lifecycle callbacks, producer methods, and CDI events — all without runtime proxies.

## Creating a Context

Use `RootContext.builder()` to create an application-scoped root context. Provide either a list of classes or a pre-built Jandex index.

```java
// From a list of classes
Context context = RootContext.builder()
        .withClasses(List.of(MyService.class, MyRepository.class))
        .build();

// From a Jandex index
Context context = RootContext.builder()
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

The container does not automatically associate subcontexts with threads or requests. The caller is responsible for tracking the active context (e.g. in a `ThreadLocal` or request attribute) and making it available through a `ProcessScopeProvider`. This provider is consulted whenever the container needs to resolve which context is current for the running thread.

```java
AtomicReference<Context> current = new AtomicReference<>();

Context root = RootContext.builder()
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

## CDI Compatibility

Blackan Injector implements a practical subset of the CDI specification. The most significant difference is the **absence of client proxies** — normal-scoped beans are not proxied, so lazy initialization and scope-crossing behavior differs from a full CDI container. Beans are created eagerly when first looked up and injected directly.

Unsupported features include:

- **Client proxies** for normal-scoped beans
- **Interceptors** and **decorators**
- **Stereotypes**
- **Bean discovery modes** (`annotated`, `all`, `none`)
- **`@Disposes`** methods for producer cleanup
- **Portable extensions** (`Extension` SPI)
- **Build-compatible extensions** (Build Compatible Extensions SPI)
- **Conversation scope** (`@ConversationScoped`)
- **`@Specializes`**
- **`Event<T>`** injectable wrapper (use `context.fire()` directly instead)
- **`BeanManager`** / `CDI.current()` programmatic lookup API
- **Bean Validation** integration
