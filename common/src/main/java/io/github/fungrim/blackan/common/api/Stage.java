package io.github.fungrim.blackan.common.api;

import java.util.Comparator;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Stage {

    BOOTSTRAP(1),
    CORE(2),
    APPLICATION(3);

    private final Integer order;

    public static Stream<Stage> orderedValues() {
        return Stream.of(values()).sorted(Comparator.comparingInt(stage -> stage.order));
    }

    public Integer order() {
        return order;
    }
}
