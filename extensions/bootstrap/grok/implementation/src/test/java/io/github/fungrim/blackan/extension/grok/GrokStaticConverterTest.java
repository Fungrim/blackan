package io.github.fungrim.blackan.extension.grok;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.extension.smallrye.config.SmallRyeConfigExtension;
import io.github.fungrim.blackan.injector.Context;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.whatap.grok.api.Grok;
import jakarta.inject.Inject;

class GrokStaticConverterTest {
    
    @ConfigMapping(prefix = "grok")
    public interface GrokConfig {

        @WithName("test")
        Grok test();
    
    }

    private Context ctx;

    @Inject
    GrokConfig config;

    @BeforeEach
    public void setup() throws IOException {
        this.ctx = Context.builder()
                .withClasses(List.of(
                    SmallRyeConfigExtension.class,
                    GrokExtension.class,
                    GrokConfig.class
                ))
                .build();
        this.ctx.decorate(this);
    }

    @Test
    public void shouldInjectConfiuration() {
        assertNotNull(config);
        assertNotNull(config.test());
    }
}
