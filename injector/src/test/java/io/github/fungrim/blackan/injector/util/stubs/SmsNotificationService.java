package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SmsNotificationService implements NotificationService {

    @Override
    public String notify(String message) {
        return "sms: " + message;
    }
}
