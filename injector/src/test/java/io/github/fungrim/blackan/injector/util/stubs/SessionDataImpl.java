package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.SessionScoped;

@SessionScoped
public class SessionDataImpl implements SessionData {

    private static int counter = 0;
    private final String sessionId;

    public SessionDataImpl() {
        this.sessionId = "session-" + (++counter);
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    public static void resetCounter() {
        counter = 0;
    }
}
