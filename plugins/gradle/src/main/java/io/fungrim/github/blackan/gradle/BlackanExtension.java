package io.fungrim.github.blackan.gradle;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class BlackanExtension {

    public abstract Property<Boolean> getIndexAll();

    public abstract ListProperty<String> getIncludes();

    public abstract ListProperty<String> getExcludes();

    public BlackanExtension() {
        getIndexAll().convention(false);
    }
}
