package io.github.fungrim.blackan.injector.event;

import java.lang.reflect.Method;
import java.util.List;

import org.jboss.jandex.MethodInfo;

import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.injector.creator.ConstructionException;
import io.github.fungrim.blackan.injector.util.Jandex;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCoordinator {

    public static void fire(Context context, List<MethodInfo> methods) {
        for (MethodInfo methodInfo : methods) {
            Method method = Jandex.toMethod(methodInfo, context.classLoader());
            Object owner = context.get(method.getDeclaringClass());
            try {
                method.setAccessible(true);
                method.invoke(owner);
                method.setAccessible(false);
            } catch (Exception e) {
                throw new ConstructionException(
                        "Failed to invoke lifecycle method " + method.getDeclaringClass().getName() + "." + method.getName(), e);
            }
        }
    }
}
