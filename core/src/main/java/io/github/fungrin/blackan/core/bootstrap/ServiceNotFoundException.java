package io.github.fungrin.blackan.core.bootstrap;

public class ServiceNotFoundException extends IllegalStateException {

    public ServiceNotFoundException(String message) {
        super(message);
    }

    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
