package io.github.fungrim.blackan.extension.grok;

import org.eclipse.microprofile.config.spi.Converter;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;

public class GrokStaticConverter implements Converter<Grok> {
    
    private final GrokCompiler compiler;

    public GrokStaticConverter() {
        this.compiler = GrokCompiler.newInstance();
        this.compiler.registerDefaultPatterns();
    }


    @Override
    public Grok convert(String value) throws IllegalArgumentException, NullPointerException {
        return StaticAccessor.get().orElseGet(
            () -> {
                System.out.println("Blackan Grok extension not initialized, will use plain Grok with default patterns");
                return compiler.compile(value);
            }
        );
    }
}
