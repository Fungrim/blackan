package io.github.fungrim.blackan.injector.util.stubs;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;

@ApplicationScoped
public class AsyncObserverBean {

    public final List<String> received = new CopyOnWriteArrayList<>();
    public final CountDownLatch latch = new CountDownLatch(1);

    public void onStringEvent(@ObservesAsync String event) {
        received.add(event);
        latch.countDown();
    }
}
