package io.github.fungrim.blackan.injector.creator;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DependentProviderStack {

    @FunctionalInterface
    public static interface Task<T> {
        T run();
    }

    private static final ThreadLocal<Set<Class<?>>> CONSTRUCTION_STACK = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Stack<Boolean>> IS_EXTENSION = ThreadLocal.withInitial(Stack::new);

    public static <T> T exec(Class<T> clazz, boolean isExtensionClass, Task<T> task) {
        if(!CONSTRUCTION_STACK.get().add(clazz)) {
            throw new ConstructionException("Circular dependency detected while constructing: " + clazz.getName());
        }
        try {
            IS_EXTENSION.get().push(isExtensionClass);
            return task.run();
        } finally {
            CONSTRUCTION_STACK.get().remove(clazz);
            IS_EXTENSION.get().pop();
        }
    }
    
    public static boolean isInExtension() {
        return IS_EXTENSION.get().isEmpty() ? false : IS_EXTENSION.get().stream().anyMatch(Boolean::booleanValue);
    }
}
