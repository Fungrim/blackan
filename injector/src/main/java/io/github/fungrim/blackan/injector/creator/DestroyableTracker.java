package io.github.fungrim.blackan.injector.creator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DestroyableTracker {

    private record Destroyable(Object instance, Method preDestroy) {}

    private final List<Destroyable> destroyables = new ArrayList<>();

    public void register(Object instance) {
        Optional<Method> preDestroy = BeanLifecycle.findPreDestroy(instance.getClass());
        preDestroy.ifPresent(m -> {
            synchronized (destroyables) {
                destroyables.add(new Destroyable(instance, m));
            }
        });
    }

    public void destroyAll() {
        List<Destroyable> snapshot;
        synchronized (destroyables) {
            snapshot = new ArrayList<>(destroyables);
            destroyables.clear();
        }
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Destroyable d = snapshot.get(i);
            BeanLifecycle.invoke(d.instance(), d.preDestroy());
        }
    }
}
