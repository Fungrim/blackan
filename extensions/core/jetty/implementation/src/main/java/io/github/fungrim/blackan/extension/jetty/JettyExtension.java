package io.github.fungrim.blackan.extension.jetty;

import java.util.List;
import java.util.concurrent.Executors;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.slf4j.Logger;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.common.cdi.RuntimeStopEvent;
import io.github.fungrim.blackan.injector.Context;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;

@Extension
@Priority(100) 
@BootStage(Stage.CORE)
public class JettyExtension {

    @Inject
    ServerConfig serverConfig;

    @Inject
    Logger log;

    private Server server;
    private ServerConnector connector;

    @Inject
    Instance<ServletContextConfig> servletContextConfig;

    @Inject
    Instance<RestContextConfig> restContextConfig;

    @Inject
    Context context;

    @Inject
    ContextServletInitializer contextServletInitializer;

    @PostConstruct
    public void init() {
        log.debug("JettyExtension initialized with config: {}", serverConfig);
    }

    public void start(@Observes RuntimeStartEvent event) throws Exception {
        log.info("JettyExtension starting");
        ThreadPool threadPool = createThreadPool();
        server = new Server(threadPool);
        connector = new ServerConnector(server);
        connector.setHost(serverConfig.bindAddress());
        connector.setPort(serverConfig.port());
        server.setConnectors(new Connector[] { connector });
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        ServletScopedContext.enter(context, () -> {
            mountServletContexts(contexts);
            server.setHandler(contexts);
            server.start();
            return null;
        });
    }

    private void mountServletContexts(ContextHandlerCollection contexts) {
        if(servletContextConfig.isUnsatisfied()) {
            log.debug("No servlet context configs found");
            if(hasServlets()) {
                log.info("Servlets found in CDI but no explicit context, will mount default context at '/'");
                ServletContextHandler ctc = new ServletContextHandler();
                ctc.setContextPath("/");
                ctc.addServletContainerInitializer(contextServletInitializer);
                ctc.setDisplayName("Default servlet context");
                mountServlets(ctc);
                contexts.addHandler(ctc);
            }
        } else {
            throw new UnsupportedOperationException("Explicit servlet context configs not yet supported");
        }
    }

    private boolean hasServlets() {
        return !getServletClasses().isEmpty();
    }

    private List<ClassInfo> getServletClasses() {
        return context.getInstance(HttpServlet.class).candidates();
    }

    private void mountServlets(ServletContextHandler ctc) {
        for (ClassInfo cl : getServletClasses()) {
            AnnotationInstance annotation = cl.annotation(ServletPath.class);
            var path = annotation != null ? annotation.value().asString() : "/";
            log.debug("Mounting servlet {} at path {}", cl.name().toString(), path);
            ctc.addServlet(cl.name().toString(), path);
        }
    }

    private ThreadPool createThreadPool() {
        var pool = new QueuedThreadPool();
        if (serverConfig.threading().maxConcurrentTasks() <= 0) {
            pool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        } else {
            VirtualThreadPool virtualExecutor = new VirtualThreadPool();
            virtualExecutor.setMaxConcurrentTasks(serverConfig.threading().maxConcurrentTasks());
            pool.setVirtualThreadsExecutor(virtualExecutor);
        }
        pool.setIdleTimeout(serverConfig.threading().idleTimeout());
        return pool;
    }

    public void stop(@Observes RuntimeStopEvent event) {
        log.info("JettyExtension stopping");
        if (server != null) {
            try {
                ServletScopedContext.enter(context, () -> {
                    server.stop();
                    return null;
                });
            } catch (Exception e) {
                log.error("Failed to stop Jetty server", e);
            }
        }
    }
}
