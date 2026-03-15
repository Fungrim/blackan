package io.github.fungrim.blackan.injector.context;

import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.creator.ConstructionException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DotNameAccessImpl implements ClassAccess {

    private final DotName name;

    @Override
    public Class<?> load(ClassLoader loader) {
        try {
            return loader.loadClass(name.toString());
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Failed to load class: " + name, e);
        }
    }

    @Override
    public DotName name() {
        return name;
    }
}
