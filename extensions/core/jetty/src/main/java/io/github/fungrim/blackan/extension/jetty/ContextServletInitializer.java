package io.github.fungrim.blackan.extension.jetty;

import java.util.Set;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

@ApplicationScoped
public class ContextServletInitializer implements ServletContainerInitializer {

    @Inject
    ContextDecorator contextDecorator;

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ServletContextHandler context = ServletContextHandler.getServletContextHandler(ctx);
        context.getObjectFactory().addDecorator(contextDecorator);
    }
}
