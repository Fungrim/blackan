package io.github.fungrim.blackan.runtime;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.github.fungrim.blackan.common.api.Initializable;
import io.github.fungrim.blackan.common.api.Service;
import io.github.fungrim.blackan.common.api.Startable;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.runtime.util.StageAndPriorityComparator;

public class RuntimeController implements Service {

    private final Context context;

    private final List<Initializable> initializables;
    private final List<Startable> startables;

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public RuntimeController(Context context) {
        this.context = context;
        this.initializables = resolveInitializableByStage();
        System.out.println("RuntimeController.initializables: " );
        initializables.forEach(i -> System.out.println("  - " + i.getClass().getName()));
        this.startables = resolveStartableByStage();
        System.out.println("RuntimeController.startables: " );
        startables.forEach(s -> System.out.println("  - " + s.getClass().getName()));
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

    public void run() throws InterruptedException {
        init();
        start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
                destroy();
            } finally {
                shutdownLatch.countDown();
            }
        }, "blackan-shutdown-hook"));
        hold();
    }

    private void hold() throws InterruptedException {
        shutdownLatch.await();
    }
}
