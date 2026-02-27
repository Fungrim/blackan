package io.github.fungrim.blackan.injector.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.github.fungrim.blackan.common.util.Arguments;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Jandex {

    public static Field toField(FieldInfo info, ClassLoader classLoader) {
        Arguments.notNull(classLoader, "ClassLoader");
        Arguments.notNull(info, "FieldInfo");
        try {
            return classLoader.loadClass(info.declaringClass().name().toString()).getDeclaredField(info.name());
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException e) {
            throw new ConstructionException("Failed to create field " + info.name() + " of " + info.declaringClass().name(), e);
        }
    }

    public static Constructor<?> toConstructor(MethodInfo info, ClassLoader classLoader) {
        Arguments.notNull(classLoader, "ClassLoader");
        Arguments.notNull(info, "MethodInfo");
        if(!info.isConstructor()) {
            throw new IllegalArgumentException("Constructor required");
        }
        try {
            Class<?> clazz = classLoader.loadClass(info.declaringClass().name().toString());
            Class<?>[] paramTypes = info.parameterTypes().stream()
                    .map(Type::asClassType)
                    .map(t -> loadType(t, classLoader))
                    .toArray(Class<?>[]::new);
            return clazz.getDeclaredConstructor(paramTypes);
        } catch (ReflectiveOperationException e) {
            throw new ConstructionException("Failed to resolve constructor " + info.name(), e);
        }
    }

    public static Method toMethod(MethodInfo info, ClassLoader classLoader) {
        Arguments.notNull(classLoader, "ClassLoader");
        Arguments.notNull(info, "MethodInfo");
        if(info.isConstructor()) {
            throw new IllegalArgumentException("Constructor not supported");
        }
        try {
            Class<?> clazz = classLoader.loadClass(info.declaringClass().name().toString());
            Class<?>[] paramTypes = info.parameterTypes().stream()
                    .map(Type::asClassType)
                    .map(t -> loadType(t, classLoader))
                    .toArray(Class<?>[]::new);
            return clazz.getDeclaredMethod(info.name(), paramTypes);
        } catch (ReflectiveOperationException e) {
            throw new ConstructionException("Failed to resolve method " + info.name(), e);
        }
    }

    private static Class<?> loadType(ClassType t, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(t.name().toString());
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Failed to load class " + t.name(), e);
        }
    }
}
