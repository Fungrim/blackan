package io.github.fungrim.blackan.extension.grok;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.whatap.grok.api.Grok;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StaticAccessor {

    private static AtomicReference<Grok> INSTANCE = new AtomicReference<>();
    
    public static Optional<Grok> get() {
        return Optional.ofNullable(INSTANCE.get());
    }
    
    static void setGrok(Grok grok) {
        INSTANCE.set(grok);
    }
}
