package io.github.fungrim.blackan.extension.jetty;

import io.github.fungrim.blackan.injector.Context;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ContextServletListener implements ServletContextListener {

    @Inject
    ContextRequestListener contextRequestListener;

    @Inject
    ContextSessionListener contextSessionListener;

    @Inject
    Context context;

    @Inject
    ServletRequestFilter servletRequestFilter;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Initializing servlet context with application scope");
        sce.getServletContext().setAttribute(Constants.APPLICATION_SCOPE_KEY.toString(), context);
        sce.getServletContext().addListener(contextRequestListener);
        sce.getServletContext().addListener(contextSessionListener);
        sce.getServletContext().addFilter("blackanRequestFilter", servletRequestFilter);
    }

    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Destroying servlet context with application scope");
        sce.getServletContext().removeAttribute(Constants.APPLICATION_SCOPE_KEY.toString());
    }
}
