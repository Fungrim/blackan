package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.common.api.Extension;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

class ExtensionScopeTest {

    private Context root;

    @Extension
    public static class TestExtension {
        
        private int instanceCounter = 0;
        
        public int getInstanceId() {
            return ++instanceCounter;
        }
        
        @Produces
        @Singleton
        public String producedValue() {
            return "value-from-instance-" + System.identityHashCode(this);
        }
    }
    
    public static class ServiceWithExtensionInjection {
        @Inject
        TestExtension extension;
    }
    
    public static class ServiceWithProducedValue {
        @Inject
        String value;
    }

    @BeforeEach
    void setup() throws IOException {
        root = Context.builder()
                .withClasses(List.of(
                        TestExtension.class,
                        ServiceWithExtensionInjection.class,
                        ServiceWithProducedValue.class))
                .build();
    }

    @Test
    void extensionBeansAreApplicationScopedByDefault() {
        TestExtension first = root.get(TestExtension.class);
        TestExtension second = root.get(TestExtension.class);
        
        assertSame(first, second, "Extension beans should be singletons");
    }
    
    @Test
    void extensionInstanceIsReusedAcrossInjections() {
        ServiceWithExtensionInjection service1 = root.get(ServiceWithExtensionInjection.class);
        ServiceWithExtensionInjection service2 = root.get(ServiceWithExtensionInjection.class);
        
        assertNotNull(service1.extension);
        assertNotNull(service2.extension);
        assertSame(service1.extension, service2.extension, 
                "Extension should be the same instance across different injection points");
    }
    
    @Test
    void producerMethodUsesTheSameExtensionInstance() {
        TestExtension extension = root.get(TestExtension.class);
        ServiceWithProducedValue service = root.get(ServiceWithProducedValue.class);
        
        String expectedValue = "value-from-instance-" + System.identityHashCode(extension);
        assertEquals(expectedValue, service.value, 
                "Producer method should be called on the same Extension instance");
    }
}
