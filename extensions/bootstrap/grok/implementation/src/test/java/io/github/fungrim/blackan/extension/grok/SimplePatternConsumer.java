package io.github.fungrim.blackan.extension.grok;

import io.whatap.grok.api.Grok;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class SimplePatternConsumer {

    @Inject
    @Pattern("%{IP}")
    private Grok grok;

    Grok grok() {
        return grok;
    }
}
