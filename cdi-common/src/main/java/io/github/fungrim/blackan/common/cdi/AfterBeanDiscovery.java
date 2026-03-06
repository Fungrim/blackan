package io.github.fungrim.blackan.common.cdi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class AfterBeanDiscovery {

    public record SyntheticBean<T>(
        Class<T> type,
        Class<? extends Annotation> scope,
        Supplier<T> factory,
        Annotation[] qualifiers
    ) {}

    private final List<SyntheticBean<?>> beans = new ArrayList<>();

    public <T> void addBean(Class<T> type, Class<? extends Annotation> scope, Supplier<T> factory, Annotation... qualifiers) {
        beans.add(new SyntheticBean<>(type, scope, factory, qualifiers != null ? qualifiers : new Annotation[0]));
    }

    public List<SyntheticBean<?>> beans() {
        return Collections.unmodifiableList(beans);
    }
}
