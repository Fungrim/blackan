package io.github.fungrim.blackan.injector.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.creator.InvocationUtil;
import io.github.fungrim.blackan.injector.lookup.RecursionKey;
import io.github.fungrim.blackan.injector.util.Jandex;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCoordinator {

    public static void fireObservers(Context context, List<ObserverMethod> observers, Object event) {
        for (ObserverMethod observer : observers) {
            invokeObserver(context, observer, event);
        }
    }

    public static CompletionStage<Object> fireObserversAsync(Context context, ExecutorService executor, List<ObserverMethod> observers, Object event) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            for (ObserverMethod observer : observers) {
                invokeObserver(context, observer, event);
            }
            return event;
        }, executor);
    }

    private static void invokeObserver(Context context, ObserverMethod observer, Object event) {
        Method method = Jandex.toMethod(observer.method(), context.classLoader());
        Object owner = context.get(method.getDeclaringClass());
        try {
            method.setAccessible(true);
            Object[] args = resolveObserverParameters(context, method, event);
            method.invoke(owner, args);
            method.setAccessible(false);
        } catch (Exception e) {
            throw new ConstructionException(
                    "Failed to invoke observer method " + method.getDeclaringClass().getName() + "." + method.getName(), e);
        }
    }

    private static Object[] resolveObserverParameters(Context context, Method method, Object event) {
        int count = method.getParameterCount();
        Object[] args = new Object[count];
        args[0] = event;
        if (count > 1) {
            Type[] genericTypes = method.getGenericParameterTypes();
            for (int i = 1; i < count; i++) {
                Class<?> paramType = method.getParameterTypes()[i];
                Annotation[] annotations = method.getParameterAnnotations()[i];
                RecursionKey key = RecursionKey.of(paramType, annotations);
                args[i] = InvocationUtil.resolveInjectionPoint(context, key, genericTypes[i]);
            }
        }
        return args;
    }

    public static List<AnnotationInstance> toJandexQualifiers(Annotation[] qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            return List.of();
        }
        List<AnnotationInstance> result = new ArrayList<>();
        for (Annotation qualifier : qualifiers) {
            DotName name = DotName.createSimple(qualifier.annotationType());
            List<org.jboss.jandex.AnnotationValue> values = extractAnnotationValues(qualifier);
            result.add(AnnotationInstance.create(name, null, values));
        }
        return result;
    }

    private static List<org.jboss.jandex.AnnotationValue> extractAnnotationValues(Annotation annotation) {
        List<org.jboss.jandex.AnnotationValue> values = new ArrayList<>();
        for (java.lang.reflect.Method member : annotation.annotationType().getDeclaredMethods()) {
            if (member.getParameterCount() != 0 || member.getDeclaringClass() == Annotation.class) {
                continue;
            }
            try {
                Object val = member.invoke(annotation);
                if (val instanceof Class<?> clazz) {
                    values.add(org.jboss.jandex.AnnotationValue.createClassValue(member.getName(),
                            org.jboss.jandex.Type.create(DotName.createSimple(clazz), org.jboss.jandex.Type.Kind.CLASS)));
                }
            } catch (Exception e) {
                // skip non-extractable values
            }
        }
        return values;
    }
}
