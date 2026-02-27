package io.github.fungrin.blackan.core.runtime;

import java.util.List;

import io.github.fungrim.blackan.common.api.Initializable;
import io.github.fungrim.blackan.common.api.Service;
import io.github.fungrim.blackan.common.api.Startable;
import io.github.fungrim.blackan.injector.Context;

public class RuntimeController implements Service {

    private final Context context;

    private final List<Initializable> initializables;
    private final List<Startable> startables;

    public RuntimeController(Context context) {
        this.context = context;
        this.initializables = resolveInitializableByStage();
        this.startables = resolveStartableByStage();
    }

    private List<Startable> resolveStartableByStage() {
        return context.getInstance(Startable.class)
                .candidates().stream().map(candidate -> {
                    return context.getInstance(candidate);
                })
                .sorted(new StageAndPriorityComparator())
                .map(instance -> {
                    return instance.get(Startable.class);
                })
                .toList();
    }

    private List<Initializable> resolveInitializableByStage() {
        return context.getInstance(Initializable.class)
                .candidates().stream().map(candidate -> {
                    return context.getInstance(candidate);
                })
                .sorted(new StageAndPriorityComparator())
                .map(instance -> {
                    return instance.get(Initializable.class);
                })
                .toList();
    }

    @Override
    public void init() {
        initializables.forEach(Initializable::init);
    }

    @Override
    public void destroy() {
        initializables.reversed().forEach(Initializable::destroy);
    }

    @Override
    public void start() {
        startables.forEach(Startable::start);
    }

    @Override
    public void stop() {
        startables.reversed().forEach(Startable::stop);
    }   
}
