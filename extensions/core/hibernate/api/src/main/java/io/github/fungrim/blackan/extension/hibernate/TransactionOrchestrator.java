package io.github.fungrim.blackan.extension.hibernate;

import org.hibernate.StatelessSession;

import io.github.fungrim.blackan.extension.hibernate.TransactionExecutor;

public interface TransactionOrchestrator {

    public TransactionExecutor create(StatelessSession session);
    
}
