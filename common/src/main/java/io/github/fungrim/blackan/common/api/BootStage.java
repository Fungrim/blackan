package io.github.fungrim.blackan.common.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

@Qualifier
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BootStage {

    public class Literal extends AnnotationLiteral<BootStage> implements BootStage {

        public static final Literal INSTANCE = new Literal();

        @Override
        public Stage value() {
            return null;
        }
    }

    Stage value();

}
