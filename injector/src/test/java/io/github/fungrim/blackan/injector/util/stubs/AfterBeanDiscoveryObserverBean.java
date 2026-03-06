package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.common.cdi.AfterBeanDiscovery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AfterBeanDiscoveryObserverBean {

    public void onAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        ContainerInitEventTracker.record("afterBeanDiscovery");
    }
}
