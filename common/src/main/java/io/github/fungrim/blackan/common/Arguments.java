package io.github.fungrim.blackan.common;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Arguments {

    public static final void notNull(Object object, String entityName) {
        if (Objects.isNull(object)) {
            throw new IllegalArgumentException(entityName + " must not be null");
        }
    }
}
