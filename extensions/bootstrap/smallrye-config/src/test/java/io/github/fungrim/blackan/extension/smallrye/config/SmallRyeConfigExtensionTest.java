package io.github.fungrim.blackan.extension.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.Context;
import io.smallrye.config.ConfigValue;

class SmallRyeConfigExtensionTest {

    @BeforeEach
    void setSystemProperties() {
        System.setProperty("test.string", "hello");
        System.setProperty("test.integer", "42");
        System.setProperty("test.long", "123456789");
        System.setProperty("test.boolean", "true");
        System.setProperty("test.double", "3.14");
    }

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("test.string");
        System.clearProperty("test.integer");
        System.clearProperty("test.long");
        System.clearProperty("test.boolean");
        System.clearProperty("test.double");
        System.clearProperty("server.host");
        System.clearProperty("server.port");
    }

    @Test
    void injectsStringConfigProperty() throws IOException {
        try (Context ctx = Context.builder()
                .withClasses(List.of(SmallRyeConfigExtension.class, StringConsumer.class))
                .build()) {
            StringConsumer bean = ctx.get(StringConsumer.class);
            assertNotNull(bean);
            assertEquals("hello", bean.value());
        }
    }

    @Test
    void injectsIntegerConfigProperty() throws IOException {
        try (Context ctx = Context.builder()
                .withClasses(List.of(SmallRyeConfigExtension.class, IntegerConsumer.class))
                .build()) {
            assertEquals(42, ctx.get(IntegerConsumer.class).value());
        }
    }

    @Test
    void injectsBooleanConfigProperty() throws IOException {
        try (Context ctx = Context.builder()
                .withClasses(List.of(SmallRyeConfigExtension.class, BooleanConsumer.class))
                .build()) {
            assertEquals(true, ctx.get(BooleanConsumer.class).value());
        }
    }

    @Test
    void usesDefaultValueWhenPropertyAbsent() throws IOException {
        try (Context ctx = Context.builder()
                .withClasses(List.of(SmallRyeConfigExtension.class, DefaultValueConsumer.class))
                .build()) {
            assertEquals("default-value", ctx.get(DefaultValueConsumer.class).value());
        }
    }

    @Test
    void injectsConfigValue() throws IOException {
        try (Context ctx = Context.builder()
                .withClasses(List.of(SmallRyeConfigExtension.class, ConfigValueConsumer.class))
                .build()) {
            ConfigValue cv = ctx.get(ConfigValueConsumer.class).value();
            assertNotNull(cv);
            assertEquals("test.string", cv.getName());
            assertEquals("hello", cv.getValue());
        }
    }

    @Test
    void injectsConfigMapping() throws IOException {
        System.setProperty("server.host", "localhost");
        System.setProperty("server.port", "8080");
        try (Context ctx = Context.builder()
                .withClasses(List.of(SmallRyeConfigExtension.class, ServerConfig.class, ServerConfigConsumer.class))
                .build()) {
            ServerConfig cfg = ctx.get(ServerConfigConsumer.class).config();
            assertNotNull(cfg);
            assertEquals("localhost", cfg.host());
            assertEquals(8080, cfg.port());
        }
    }

    @Test
    void failsDeploymentWhenRequiredPropertyMissing() {
        assertThrows(Exception.class, () ->
            Context.builder()
                .withClasses(List.of(SmallRyeConfigExtension.class, MissingPropertyConsumer.class))
                .build()
        );
    }
}
