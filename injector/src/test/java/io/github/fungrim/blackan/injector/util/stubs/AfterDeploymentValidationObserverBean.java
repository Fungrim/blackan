package io.github.fungrim.blackan.injector.util.stubs;

import io.github.fungrim.blackan.common.cdi.AfterDeploymentValidation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AfterDeploymentValidationObserverBean {

    public void onAfterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        ContainerInitEventTracker.record("afterDeploymentValidation");
    }
}
