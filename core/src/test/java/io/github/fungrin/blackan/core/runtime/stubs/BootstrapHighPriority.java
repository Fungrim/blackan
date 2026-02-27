package io.github.fungrin.blackan.core.runtime.stubs;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Stage;
import jakarta.annotation.Priority;

@BootStage(Stage.BOOTSTRAP)
@Priority(1)
public class BootstrapHighPriority {
}
