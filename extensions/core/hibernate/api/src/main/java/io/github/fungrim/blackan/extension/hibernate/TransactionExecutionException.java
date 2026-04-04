package io.github.fungrim.blackan.extension.hibernate;

public class TransactionExecutionException extends RuntimeException {

    public TransactionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
