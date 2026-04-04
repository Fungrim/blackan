package io.github.fungrim.blackan.injector;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

class ProducerProviderInjectionTest {

    private final AtomicReference<Context> currentContext = new AtomicReference<>();
    private Context root;

    public interface DatabaseConnection {
        String getConnectionString();
    }

    public static class DatabaseConnectionImpl implements DatabaseConnection {
        private final String connectionString;

        public DatabaseConnectionImpl(String connectionString) {
            this.connectionString = connectionString;
        }

        @Override
        public String getConnectionString() {
            return connectionString;
        }
    }

    @ApplicationScoped
    public static class DatabaseProducer {

        @Default
        @Produces
        @Singleton
        public DatabaseConnection databaseConnection() {
            return new DatabaseConnectionImpl("jdbc:postgresql://localhost:5432/testdb");
        }
    }

    @ApplicationScoped
    public static class ServiceWithDirectInjection {
        @Inject
        DatabaseConnection connection;
    }

    @ApplicationScoped
    public static class ServiceWithProviderInjection {
        @Inject
        Provider<DatabaseConnection> connectionProvider;
    }

    @BeforeEach
    void setup() throws IOException {
        root = Context.builder()
                .withClasses(List.of(
                        DatabaseConnection.class,
                        DatabaseConnectionImpl.class,
                        DatabaseProducer.class,
                        ServiceWithDirectInjection.class,
                        ServiceWithProviderInjection.class))
                .withScopeProvider(() -> currentContext.get())
                .build();
        currentContext.set(root);
    }

    @Test
    void directInjectionOfProducedInterfaceWorks() {
        ServiceWithDirectInjection service = root.get(ServiceWithDirectInjection.class);
        assertNotNull(service);
        assertNotNull(service.connection);
        assertNotNull(service.connection.getConnectionString());
    }

    @Test
    void providerInjectionOfProducedInterfaceWorks() {
        ServiceWithProviderInjection service = root.get(ServiceWithProviderInjection.class);
        assertNotNull(service);
        assertNotNull(service.connectionProvider);
        
        DatabaseConnection connection = service.connectionProvider.get();
        assertNotNull(connection);
        assertNotNull(connection.getConnectionString());
    }

    @Test
    void providerReturnsTheSameInstanceAsDirectInjection() {
        ServiceWithDirectInjection directService = root.get(ServiceWithDirectInjection.class);
        ServiceWithProviderInjection providerService = root.get(ServiceWithProviderInjection.class);
        
        DatabaseConnection fromDirect = directService.connection;
        DatabaseConnection fromProvider = providerService.connectionProvider.get();
        
        assertSame(fromDirect, fromProvider);
    }
}
