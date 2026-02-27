package io.github.fungrin.blackan.core.bootstrap;

import java.io.IOException;

import org.jboss.jandex.Index;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.context.RootContext;
import io.github.fungrin.blackan.core.runtime.RuntimeController;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceBootstrap {

    public static RuntimeController bootstrap(Index index) throws IOException {
        Context context = RootContext.builder().withIndex(index).build();
        return new RuntimeController(context);
    }
}
