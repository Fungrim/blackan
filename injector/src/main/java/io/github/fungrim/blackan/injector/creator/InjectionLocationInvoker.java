package io.github.fungrim.blackan.injector.creator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.github.fungrim.blackan.injector.util.Jandex;

public interface InjectionLocationInvoker {

    public static InjectionLocationInvoker of(MethodInfo method, ClassLoader classLoader) {
        if(method.isConstructor()) {
            final Constructor<?> c = Jandex.toConstructor(method, classLoader);
            return (opt, params) -> {
                try {
                    c.setAccessible(true);
                    c.newInstance(params);
                    c.setAccessible(false);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new ConstructionException("Failed to invoke constructor " + method.name(), e);
                }
            };
        } else {
            final Method m = Jandex.toMethod(method, classLoader);
            return (opt, params) -> {
                try {
                    m.setAccessible(true);
                    m.invoke(opt.orElse(null), params);
                    m.setAccessible(false);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ConstructionException("Failed to invoke method " + method.name(), e);
                }
            };
        }
    } 

    public static InjectionLocationInvoker of(FieldInfo field, ClassLoader classLoader) {
        final Field f = Jandex.toField(field, classLoader); 
        return (opt, params) -> {
            try {
                f.setAccessible(true);
                f.set(opt.orElse(null), params[0]);
                f.setAccessible(false);
            } catch (IllegalAccessException e) {
                throw new ConstructionException("Failed to set field " + field.name(), e);
            }
        };
    }

    public void invoke(Optional<Object> parentInstance, Object[] parameters);

}
