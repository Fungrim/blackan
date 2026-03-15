package io.github.fungrim.blackan.test;

import io.github.fungrim.blackan.extension.jackson.ObjectMapperConfigurer;
import jakarta.enterprise.context.ApplicationScoped;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@ApplicationScoped
public class ObjectMapperMod implements ObjectMapperConfigurer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.configure(SerializationFeature.INDENT_OUTPUT, true);
    }    
}
