package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class AmbiguousInstanceBean {

    @Inject
    public Instance<NotificationService> notificationServices;
}
