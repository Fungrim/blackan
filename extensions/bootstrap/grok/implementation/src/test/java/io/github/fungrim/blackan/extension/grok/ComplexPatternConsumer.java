package io.github.fungrim.blackan.extension.grok;

import io.whatap.grok.api.Grok;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ComplexPatternConsumer {

    @Inject
    @Pattern("%{IP:client} %{WORD:method} %{URIPATHPARAM:request} %{NUMBER:bytes} %{NUMBER:duration}")
    private Grok grok;

    Grok grok() {
        return grok;
    }
}
