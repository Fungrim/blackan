package io.github.fungrim.blackan.extension.grok;

import io.whatap.grok.api.Grok;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class OptionalPatternConsumer {

    @Inject
    @Nullable
    private Grok grok;

    Grok grok() {
        return grok;
    }
}
