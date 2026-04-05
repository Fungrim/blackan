package io.github.fungrim.blackan.extension.grok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.extension.smallrye.config.SmallRyeConfigExtension;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.whatap.grok.api.Match;

class GrokExtensionTest {

    private Context ctx;

    @BeforeEach
    public void setup() throws IOException {
        this.ctx = Context.builder()
                .withClasses(List.of(
                    SmallRyeConfigExtension.class,
                    GrokExtension.class,
                    SimplePatternConsumer.class,
                    ComplexPatternConsumer.class,
                    OptionalPatternConsumer.class,
                    MissingPatternConsumer.class,
                    EmptyPatternConsumer.class))
                .build();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("blackan.grok.disable-default-patterns");
    }

    @Test
    void injectsGrokWithSimplePattern() {
        SimplePatternConsumer consumer = ctx.get(SimplePatternConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.grok());
        
        Match result = consumer.grok().match("127.0.0.1");
        assertNotNull(result);
        assertEquals("127.0.0.1", result.capture().get("IP"));
    }

    @Test
    void injectsGrokWithComplexPattern() {
        ComplexPatternConsumer consumer = ctx.get(ComplexPatternConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.grok());
        
        String logLine = "55.3.244.1 GET /index.html 15824 0.043";
        Match result = consumer.grok().match(logLine);
        assertNotNull(result);
        assertEquals("55.3.244.1", result.capture().get("client"));
        assertEquals("GET", result.capture().get("method"));
        assertEquals("/index.html", result.capture().get("request"));
        assertEquals("15824", result.capture().get("bytes"));
        assertEquals("0.043", result.capture().get("duration"));
    }

    @Test
    void injectsNullForOptionalGrokWithoutPattern() {
        OptionalPatternConsumer consumer = ctx.get(OptionalPatternConsumer.class);
        assertNotNull(consumer);
        assertNull(consumer.grok());
    }

    @Test
    void throwsOnMissingPatternAnnotation() {
        assertThrows(ConstructionException.class, () -> {
            ctx.get(MissingPatternConsumer.class);
        });
    }

    @Test
    void throwsOnEmptyPatternValue() {
        assertThrows(ConstructionException.class, () -> {
            ctx.get(EmptyPatternConsumer.class);
        });
    }

    @Test
    void extensionInitializesSuccessfully() {
        GrokExtension extension = ctx.get(GrokExtension.class);
        assertNotNull(extension);
    }
}
