package io.github.fungrim.blackan.runtime;

import java.util.concurrent.CountDownLatch;

import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.common.cdi.RuntimeStopEvent;
import io.github.fungrim.blackan.injector.Context;

public class RuntimeController {

    private final Context context;

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public RuntimeController(Context context) {
        this.context = context;
    }

    public void start() {
        context.fireInOrder(new RuntimeStartEvent());
    }

    public void stop() {
        context.fireInReverseOrder(new RuntimeStopEvent());
    }   

    public void run() throws InterruptedException {
        start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
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
