package io.github.fungrim.blackan.injector.util.stubs;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestInfoImpl implements RequestInfo {

    private static int counter = 0;
    private final String requestId;

    public RequestInfoImpl() {
        this.requestId = "request-" + (++counter);
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    public static void resetCounter() {
        counter = 0;
    }
}
