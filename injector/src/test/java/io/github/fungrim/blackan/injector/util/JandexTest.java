package io.github.fungrim.blackan.injector.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.injector.util.stubs.SampleBean;

class JandexTest {

    private static Index index;
    private static ClassInfo classInfo;
    private static final ClassLoader CLASS_LOADER = JandexTest.class.getClassLoader();

    @BeforeAll
    static void buildIndex() throws IOException {
        Indexer indexer = new Indexer();
        indexer.indexClass(SampleBean.class);
        index = indexer.complete();
        classInfo = index.getClassByName(SampleBean.class.getName());
    }

    @Nested
    class ToField {

        @Test
        void resolvesPublicField() {
            var fieldInfo = classInfo.field("name");
            Field field = Jandex.toField(fieldInfo, CLASS_LOADER);
            assertNotNull(field);
            assertEquals("name", field.getName());
            assertEquals(String.class, field.getType());
        }

        @Test
        void resolvesPrivateField() {
            var fieldInfo = classInfo.field("count");
            Field field = Jandex.toField(fieldInfo, CLASS_LOADER);
            assertNotNull(field);
            assertEquals("count", field.getName());
            assertEquals(Integer.class, field.getType());
        }
    }

    @Nested
    class ToConstructor {

        @Test
        void resolvesNoArgConstructor() {
            var ctorInfo = classInfo.constructors().stream()
                    .filter(m -> m.parametersCount() == 0)
                    .findFirst().orElseThrow();
            Constructor<?> ctor = Jandex.toConstructor(ctorInfo, CLASS_LOADER);
            assertNotNull(ctor);
            assertEquals(0, ctor.getParameterCount());
        }

        @Test
        void resolvesParameterizedConstructor() {
            var ctorInfo = classInfo.constructors().stream()
                    .filter(m -> m.parametersCount() == 2)
                    .findFirst().orElseThrow();
            Constructor<?> ctor = Jandex.toConstructor(ctorInfo, CLASS_LOADER);
            assertNotNull(ctor);
            assertEquals(2, ctor.getParameterCount());
            assertEquals(String.class, ctor.getParameterTypes()[0]);
            assertEquals(Integer.class, ctor.getParameterTypes()[1]);
        }

        @Test
        void throwsForNonConstructorMethodInfo() {
            var methodInfo = classInfo.methods().stream()
                    .filter(m -> m.name().equals("init"))
                    .findFirst().orElseThrow();
            assertThrows(IllegalArgumentException.class,
                    () -> Jandex.toConstructor(methodInfo, CLASS_LOADER));
        }
    }

    @Nested
    class ToMethod {

        @Test
        void resolvesSingleParamMethod() {
            var methodInfo = classInfo.methods().stream()
                    .filter(m -> m.name().equals("init"))
                    .findFirst().orElseThrow();
            Method method = Jandex.toMethod(methodInfo, CLASS_LOADER);
            assertNotNull(method);
            assertEquals("init", method.getName());
            assertEquals(1, method.getParameterCount());
            assertEquals(String.class, method.getParameterTypes()[0]);
        }

        @Test
        void resolvesMultiParamMethod() {
            var methodInfo = classInfo.methods().stream()
                    .filter(m -> m.name().equals("setup"))
                    .findFirst().orElseThrow();
            Method method = Jandex.toMethod(methodInfo, CLASS_LOADER);
            assertNotNull(method);
            assertEquals("setup", method.getName());
            assertEquals(2, method.getParameterCount());
            assertEquals(String.class, method.getParameterTypes()[0]);
            assertEquals(Integer.class, method.getParameterTypes()[1]);
        }

        @Test
        void throwsForConstructorMethodInfo() {
            var ctorInfo = classInfo.constructors().stream()
                    .filter(m -> m.parametersCount() == 0)
                    .findFirst().orElseThrow();
            assertThrows(IllegalArgumentException.class,
                    () -> Jandex.toMethod(ctorInfo, CLASS_LOADER));
        }
    }
}
