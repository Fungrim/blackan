package io.github.fungrim.blackan.injector.context;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClassInfoAccessImpl implements Context.ClassAccess {

    private final ClassInfo info;

    @Override
    public Class<?> load(ClassLoader loader) {
        try {
            return loader.loadClass(info.name().toString());
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Failed to load class: " + info.name(), e);
        }
    }

    @Override
    public DotName name() {
        return info.name();
    }

    @Override
    public boolean isInterface() {
        return info.isInterface();
    }
}
