package io.github.fungrim.blackan.extension.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;

@Extension
@Priority(1) // we need to load the log classes asap
@BootStage(Stage.BOOTSTRAP)
public class Slf4jExtension {

    static {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
    }

    @Produces
    TargetAwareProvider<Logger> getLogger() {
        return (target) -> LoggerFactory.getLogger(target.parentClass());
    }
}
