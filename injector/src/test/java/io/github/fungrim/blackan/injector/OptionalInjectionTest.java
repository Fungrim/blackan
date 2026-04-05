package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

class OptionalInjectionTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();
    private Context root;

    @BeforeEach
    void setup() throws IOException {
        root = Context.builder()
                .withClasses(List.of())
                .withScopeProvider(() -> currentContext.get())
                .build();
        currentContext.set(root);
    }

    @AfterEach
    void teardown() {
        if (root != null) {
            root.close();
        }
    }

    @Nested
    class OptionalBeanInjection {

        @Test
        void injectsOptionalWithExistingBean() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(ServiceWithOptionalDependency.class, ExistingService.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalDependency service = ctx.get(ServiceWithOptionalDependency.class);
            assertNotNull(service);
            assertTrue(service.optionalService.isPresent());
            assertEquals("existing", service.optionalService.get().getValue());
        }

        @Test
        void injectsEmptyOptionalWhenBeanDoesNotExist() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(ServiceWithOptionalDependency.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalDependency service = ctx.get(ServiceWithOptionalDependency.class);
            assertNotNull(service);
            assertTrue(service.optionalService.isEmpty());
        }

        @Test
        void injectsEmptyOptionalWhenBeanIsAmbiguous() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(
                            ServiceWithOptionalDependency.class,
                            ExistingService.class,
                            AnotherExistingService.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalDependency service = ctx.get(ServiceWithOptionalDependency.class);
            assertNotNull(service);
            assertTrue(service.optionalService.isEmpty());
        }
    }

    @Nested
    class OptionalProducerInjection {

        @Test
        void injectsOptionalFromProducer() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(ServiceWithOptionalString.class, StringProducer.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalString service = ctx.get(ServiceWithOptionalString.class);
            assertNotNull(service);
            assertTrue(service.optionalGreeting.isPresent());
            assertEquals("Hello from producer", service.optionalGreeting.get());
        }

        @Test
        void injectsEmptyOptionalWhenProducerDoesNotExist() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(ServiceWithOptionalString.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalString service = ctx.get(ServiceWithOptionalString.class);
            assertNotNull(service);
            assertTrue(service.optionalGreeting.isEmpty());
        }
    }

    @Nested
    class OptionalTargetAwareProviderInjection {

        @Test
        void injectsOptionalFromTargetAwareProvider() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(
                            ServiceWithOptionalTargetAware.class,
                            TargetAwareStringProducer.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalTargetAware service = ctx.get(ServiceWithOptionalTargetAware.class);
            assertNotNull(service);
            assertTrue(service.optionalMessage.isPresent());
            assertEquals("Message for ServiceWithOptionalTargetAware.optionalMessage", 
                    service.optionalMessage.get());
        }

        @Test
        void injectsEmptyOptionalWhenTargetAwareReturnsNull() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(
                            ServiceWithOptionalTargetAware.class,
                            ConditionalTargetAwareProducer.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalTargetAware service = ctx.get(ServiceWithOptionalTargetAware.class);
            assertNotNull(service);
            assertTrue(service.optionalMessage.isEmpty());
        }

        @Test
        void targetAwareProviderReceivesIsOptionalTrue() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(
                            ServiceWithOptionalTargetAware.class,
                            IsOptionalCheckingProducer.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithOptionalTargetAware service = ctx.get(ServiceWithOptionalTargetAware.class);
            assertNotNull(service);
            assertTrue(service.optionalMessage.isPresent());
            assertEquals("isOptional=true", service.optionalMessage.get());
        }
    }

    @Nested
    class OptionalWithQualifiers {

        @Test
        void injectsOptionalWithQualifier() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(
                            ServiceWithQualifiedOptional.class,
                            QualifiedStringProducer.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithQualifiedOptional service = ctx.get(ServiceWithQualifiedOptional.class);
            assertNotNull(service);
            assertTrue(service.optionalSpecial.isPresent());
            assertEquals("special value", service.optionalSpecial.get());
        }

        @Test
        void injectsEmptyOptionalWhenQualifierDoesNotMatch() throws IOException {
            Context ctx = Context.builder()
                    .withClasses(List.of(ServiceWithQualifiedOptional.class))
                    .withScopeProvider(() -> currentContext.get())
                    .build();
            currentContext.set(ctx);

            ServiceWithQualifiedOptional service = ctx.get(ServiceWithQualifiedOptional.class);
            assertNotNull(service);
            assertTrue(service.optionalSpecial.isEmpty());
        }
    }

    @ApplicationScoped
    public static class ServiceWithOptionalDependency {
        @Inject
        public Optional<ExistingService> optionalService;
    }

    @ApplicationScoped
    public static class ExistingService {
        public String getValue() {
            return "existing";
        }
    }

    @ApplicationScoped
    public static class AnotherExistingService extends ExistingService {
        @Override
        public String getValue() {
            return "another";
        }
    }

    @ApplicationScoped
    public static class ServiceWithOptionalString {
        @Inject
        @Named("greeting")
        public Optional<String> optionalGreeting;
    }

    @ApplicationScoped
    public static class StringProducer {
        @Produces
        @Named("greeting")
        public String produceGreeting() {
            return "Hello from producer";
        }
    }

    @ApplicationScoped
    public static class ServiceWithOptionalTargetAware {
        @Inject
        @Named("target-aware")
        public Optional<String> optionalMessage;
    }

    @ApplicationScoped
    public static class TargetAwareStringProducer {
        @Produces
        @Named("target-aware")
        public TargetAwareProvider<String> produceTargetAware() {
            return (target, isOptional) -> "Message for " + target.parentClass().getSimpleName() 
                    + "." + target.targetName();
        }
    }

    @ApplicationScoped
    public static class ConditionalTargetAwareProducer {
        @Produces
        @Named("target-aware")
        public TargetAwareProvider<String> produceConditional() {
            return (target, isOptional) -> {
                if (isOptional) {
                    return null;
                }
                return "non-optional value";
            };
        }
    }

    @ApplicationScoped
    public static class IsOptionalCheckingProducer {
        @Produces
        @Named("target-aware")
        public TargetAwareProvider<String> produceWithCheck() {
            return (target, isOptional) -> "isOptional=" + isOptional;
        }
    }

    @ApplicationScoped
    public static class ServiceWithQualifiedOptional {
        @Inject
        @Named("special")
        public Optional<String> optionalSpecial;
    }

    @ApplicationScoped
    public static class QualifiedStringProducer {
        @Produces
        @Named("special")
        public String produceSpecial() {
            return "special value";
        }
    }
}
