package io.github.fungrim.blackan.common.cdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.inject.Named;
import jakarta.inject.Qualifier;

class InjectionPointTest {

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestQualifier {
    }

    static class TestClass {
        @Named("testField")
        @TestQualifier
        private String simpleField;

        @Named("genericField")
        private List<String> genericField;

        private Integer noAnnotationField;

        public TestClass(@Named("param1") String param1, @TestQualifier Integer param2) {
        }

        public void testMethod(@Named("methodParam") String param, List<Integer> genericParam) {
        }
    }

    @Test
    void testOfConstructorParameter() throws Exception {
        Constructor<?> constructor = TestClass.class.getDeclaredConstructor(String.class, Integer.class);
        Parameter param1 = constructor.getParameters()[0];
        Parameter param2 = constructor.getParameters()[1];

        InjectionPoint ip1 = InjectionPoint.of(constructor, param1);
        assertEquals(TestClass.class, ip1.parentClass());
        assertEquals(InjectionPointType.CONSTRUCTOR, ip1.injectPointType());
        assertEquals("arg0", ip1.targetName());
        assertEquals(String.class, ip1.targetType());
        assertTrue(ip1.targetTypeArguments().isEmpty());
        assertEquals(1, ip1.qualifiers().size());
        assertTrue(ip1.hasAnnotation(Named.class));

        InjectionPoint ip2 = InjectionPoint.of(constructor, param2);
        assertEquals(TestClass.class, ip2.parentClass());
        assertEquals(InjectionPointType.CONSTRUCTOR, ip2.injectPointType());
        assertEquals("arg1", ip2.targetName());
        assertEquals(Integer.class, ip2.targetType());
        assertTrue(ip2.targetTypeArguments().isEmpty());
        assertEquals(1, ip2.qualifiers().size());
        assertTrue(ip2.hasAnnotation(TestQualifier.class));
    }

    @Test
    void testOfField() throws Exception {
        Field simpleField = TestClass.class.getDeclaredField("simpleField");
        InjectionPoint ip = InjectionPoint.of(simpleField);

        assertEquals(TestClass.class, ip.parentClass());
        assertEquals(InjectionPointType.FIELD, ip.injectPointType());
        assertEquals("simpleField", ip.targetName());
        assertEquals(String.class, ip.targetType());
        assertTrue(ip.targetTypeArguments().isEmpty());
        assertEquals(2, ip.qualifiers().size());
        assertTrue(ip.hasAnnotation(Named.class));
        assertTrue(ip.hasAnnotation(TestQualifier.class));
        assertEquals("testField", ip.getAnnotation(Named.class).map(a -> ((Named) a).value()).orElse(null));
    }

    @Test
    void testOfFieldWithGenerics() throws Exception {
        Field genericField = TestClass.class.getDeclaredField("genericField");
        InjectionPoint ip = InjectionPoint.of(genericField);

        assertEquals(TestClass.class, ip.parentClass());
        assertEquals(InjectionPointType.FIELD, ip.injectPointType());
        assertEquals("genericField", ip.targetName());
        assertEquals(List.class, ip.targetType());
        assertEquals(1, ip.targetTypeArguments().size());
        assertEquals(String.class, ip.targetTypeArguments().get(0));
        assertEquals(1, ip.qualifiers().size());
        assertTrue(ip.hasAnnotation(Named.class));
    }

    @Test
    void testOfFieldWithNoAnnotations() throws Exception {
        Field noAnnotationField = TestClass.class.getDeclaredField("noAnnotationField");
        InjectionPoint ip = InjectionPoint.of(noAnnotationField);

        assertEquals(TestClass.class, ip.parentClass());
        assertEquals(InjectionPointType.FIELD, ip.injectPointType());
        assertEquals("noAnnotationField", ip.targetName());
        assertEquals(Integer.class, ip.targetType());
        assertTrue(ip.targetTypeArguments().isEmpty());
        assertTrue(ip.qualifiers().isEmpty());
        assertFalse(ip.hasAnnotation(Named.class));
    }

    @Test
    void testOfMethodParameter() throws Exception {
        Method method = TestClass.class.getDeclaredMethod("testMethod", String.class, List.class);
        Parameter param1 = method.getParameters()[0];
        Parameter param2 = method.getParameters()[1];

        InjectionPoint ip1 = InjectionPoint.of(method, param1);
        assertEquals(TestClass.class, ip1.parentClass());
        assertEquals(InjectionPointType.METHOD, ip1.injectPointType());
        assertEquals("arg0", ip1.targetName());
        assertEquals(String.class, ip1.targetType());
        assertTrue(ip1.targetTypeArguments().isEmpty());
        assertEquals(1, ip1.qualifiers().size());
        assertTrue(ip1.hasAnnotation(Named.class));

        InjectionPoint ip2 = InjectionPoint.of(method, param2);
        assertEquals(TestClass.class, ip2.parentClass());
        assertEquals(InjectionPointType.METHOD, ip2.injectPointType());
        assertEquals("arg1", ip2.targetName());
        assertEquals(List.class, ip2.targetType());
        assertEquals(1, ip2.targetTypeArguments().size());
        assertEquals(Integer.class, ip2.targetTypeArguments().get(0));
        assertTrue(ip2.qualifiers().isEmpty());
    }

    @Test
    void testGetAnnotation() throws Exception {
        Field field = TestClass.class.getDeclaredField("simpleField");
        InjectionPoint ip = InjectionPoint.of(field);

        assertTrue(ip.getAnnotation(Named.class).isPresent());
        assertEquals("testField", ((Named) ip.getAnnotation(Named.class).get()).value());
        assertTrue(ip.getAnnotation(TestQualifier.class).isPresent());
        assertFalse(ip.getAnnotation(Deprecated.class).isPresent());
    }

    @Test
    void testToString() throws Exception {
        Field field = TestClass.class.getDeclaredField("simpleField");
        InjectionPoint ip = InjectionPoint.of(field);

        String str = ip.toString();
        assertTrue(str.contains("InjectionPoint"));
        assertTrue(str.contains("TestClass"));
        assertTrue(str.contains("FIELD"));
        assertTrue(str.contains("simpleField"));
    }
}
