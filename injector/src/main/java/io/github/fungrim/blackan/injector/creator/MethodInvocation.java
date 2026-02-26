package io.github.fungrim.blackan.injector.creator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MethodInvocation {

    private final Context context;
    private final Object object;
    private final Method method;
    private final RecursionKey[] parameters;
    private final Type[] genericTypes;

    public static List<MethodInvocation> of(Context context, Object object) {
        List<MethodInvocation> methodInvocations = new ArrayList<>();
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                methodInvocations.add(of(context, object, method));
            }
        }
        return methodInvocations;
    }

    public static MethodInvocation of(Context context, Object object, Method method) {
        return new MethodInvocation(context, object, method, extractInjectionPoints(method), method.getGenericParameterTypes());
    }

    private static RecursionKey[] extractInjectionPoints(Method method) {
        RecursionKey[] keys = new RecursionKey[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            Class<?> parameterType = method.getParameterTypes()[i];
            Annotation[] annotations = method.getParameterAnnotations()[i];
            keys[i] = RecursionKey.of(parameterType, annotations);
        }
        return keys;
    }

    public void invoke() {
        try {
            method.setAccessible(true);
            method.invoke(object, InvocationUtil.resolveParameters(context, parameters, genericTypes));
            method.setAccessible(false);
        } catch (Exception e) {
            throw new ConstructionException("Failed to invoke method " + method.getName(), e);
        }
    }
}
