package io.github.fungrim.blackan.injector.context;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public interface ClassAccess {

    public static ClassAccess of(final Class<?> cl) {
        return new ClassAccessImpl(cl);
    }

    public static ClassAccess of(final ClassInfo info) {
        return new ClassInfoAccessImpl(info);
    }

    public static ClassAccess of(final DotName name) {
        return new DotNameAccessImpl(name);
    }

    Class<?> load(ClassLoader loader);

    DotName name();

}