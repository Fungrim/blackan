package io.github.fungrim.blackan.injector.creator;

public class ConstructionException extends IllegalStateException {

    public ConstructionException(String message) {
        super(message);
    }

    public ConstructionException(String message, Throwable cause) {
        super(message, cause);
    }
}
