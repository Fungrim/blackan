package io.github.fungrim.blackan.extension.jackson;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Extension
@Priority(10) 
@BootStage(Stage.CORE)
public class JacksonExtension {

    @Inject
    Instance<ObjectMapperConfigurer> configurers;

    @Inject 
    ObjectMapperConfig config;

    @Default
    @Singleton
    public ObjectMapper objectMapper() {
        var builder = JsonMapper.builder();
        if(configurers.isResolvable()) {
            configurers.forEach(c -> c.customize(builder));
        } else {
            doDefaultConfiguration(builder);
        }
        return builder.build();
    }
    
    private void doDefaultConfiguration(JsonMapper.Builder builder) {
        // serialization
        builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, config.serialization().failOnEmptyBeans());
        // deserialization
        builder.configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, config.deserialization().failOnUnknownProperties());
        builder.configure(tools.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, config.deserialization().coerceEmptyStringToNull());
    }
}
