package io.github.fungrim.blackan.extension.jackson;

import tools.jackson.databind.json.JsonMapper;

public interface ObjectMapperConfigurer {

    public void customize(JsonMapper.Builder builder);
    
}
