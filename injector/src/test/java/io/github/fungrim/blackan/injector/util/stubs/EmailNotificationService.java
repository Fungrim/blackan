package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmailNotificationService implements NotificationService {

    @Override
    public String notify(String message) {
        return "email: " + message;
    }
}
