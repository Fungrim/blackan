package io.github.fungrim.blackan.extension.hibernate;

import io.github.fungrim.blackan.extension.hibernate.TransactionExecutionException;

@FunctionalInterface
public interface TransactionExecutor {

    public void execute(Runnable runnable) throws TransactionExecutionException;

}
