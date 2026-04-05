package io.github.fungrim.blackan.extension.grok;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

@Extension
@BootStage(Stage.BOOTSTRAP)
public class GrokExtension {

    @Inject
    @ConfigProperty(name = "blackan.grok.disable-default-patterns", defaultValue = "false")
    private Boolean disableDefaultPatterns;

    private GrokCompiler compiler;

    @PostConstruct
    public void init() {
        System.out.println("GrokExtension initialized");
        this.compiler = GrokCompiler.newInstance();
        if (!disableDefaultPatterns) {
            this.compiler.registerDefaultPatterns();
        }
    }

    @Produces
    TargetAwareProvider<Grok> getGrok() {
        return (target, isOptional) -> {
            var annotation = target.getAnnotation(Pattern.class);
            if (annotation.isEmpty()) {
                if(!isOptional) {
                    throw new DeploymentException("Missing @Pattern annotation on: " + target);
                }
                return null;
            }
            var value = annotation.map(a -> ((Pattern) a).value()).orElse("");
            if(value.isEmpty() || value.isBlank()) {
                if(!isOptional) {
                    throw new DeploymentException("Missing pattern value on: " + target);
                }
                return null;
            }
            return this.compiler.compile(value);
        };
    }
}
