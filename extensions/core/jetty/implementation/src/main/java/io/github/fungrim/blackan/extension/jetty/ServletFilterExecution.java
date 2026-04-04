package io.github.fungrim.blackan.extension.jetty;

import java.io.IOException;

import jakarta.servlet.ServletException;

@FunctionalInterface
public interface ServletFilterExecution extends Runnable {

    public default void run() {
        try {
            execute();
        } catch (IOException | ServletException e) {
            throw new ServletRuntimeException(e);
        }
    }

    public void execute() throws IOException, ServletException;
    
}
