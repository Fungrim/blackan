package io.github.fungrim.blackan.extension.grcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.common.cdi.RuntimeStopEvent;
import io.github.fungrim.blackan.injector.Context;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Extension
@Priority(101) 
@BootStage(Stage.CORE)
public class GrcpExtension {

    @Inject
    Context context;

    @Inject
    GrpcExtensionConfig config;

    private Server server;

    public void start(@Observes RuntimeStartEvent event) throws IOException {
        log.debug("Grcp extension starting");
        SocketAddress address = new InetSocketAddress(config.bindAddress(), config.port());
        NettyServerBuilder builder = NettyServerBuilder.forAddress(address);
        context.index().getAnnotations(DotName.createSimple(GrcpService.class)).forEach(annotation -> {
            ClassInfo target = annotation.target().asClass();
            Class<?> targetClass = context.loadClass(target.name());
            if(BindableService.class.isAssignableFrom(targetClass)) {
                log.debug("Bindable Grcp service {} added to server", target.name());
                BindableService instance = (BindableService) context.get(targetClass);
                builder.addService(instance);
            } else {
                throw new DeploymentException("Service class not instance of BindableService. Only annotate gRPC service implementations. Class: " + target.name());
            }            
        });
        builder.executor(Executors.newVirtualThreadPerTaskExecutor());
        this.server = builder.build();
        this.server.start();
        log.debug("Grcp extension started on {}", address);
    }

    public void stop(@Observes RuntimeStopEvent event) {
        log.debug("Grcp extension stopping");
        if (this.server != null) {
            this.server.shutdown();
        }
        log.debug("Grcp extension stopped");
    }
}
