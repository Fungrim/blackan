package io.github.fungrim.blackan.injector.context;

import org.jboss.jandex.DotName;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClassAccessImpl implements ClassAccess {

    private final Class<?> cl;

    @Override
    public Class<?> load(ClassLoader loader) {
        return cl;
    }

    @Override
    public DotName name() {
        return DotName.createSimple(cl);
    }
}
