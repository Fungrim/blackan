package io.github.fungrim.blackan.injector.lookup.stubs;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD, PARAMETER})
public @interface Tracked {

    String value();

    @Nonbinding
    String description() default "";

    @SuppressWarnings("all")
    public static final class Literal implements Tracked {

        private final String value;
        private final String description;

        private Literal(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public static Literal of(String value) {
            return new Literal(value, "");
        }

        public static Literal of(String value, String description) {
            return new Literal(value, description);
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Tracked.class;
        }
    }
}
