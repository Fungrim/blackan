package io.github.fungrim.blackan.extension.jackson;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "blackan.jackson.object-mapper")
public interface ObjectMapperConfig {

    Serialization serialization();
    
    Deserialization deserialization();

    public static interface Serialization {

        @WithDefault("true")
        boolean failOnEmptyBeans();
    }

    public static interface Deserialization {

        @WithDefault("false")
        boolean failOnUnknownProperties();

        @WithDefault("false")
        boolean coerceEmptyStringToNull();

    }
}
