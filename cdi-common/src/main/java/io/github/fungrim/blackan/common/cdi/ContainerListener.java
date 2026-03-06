package io.github.fungrim.blackan.common.cdi;

/**
 * Callback interface for container lifecycle events. Implement this and
 * register it via {@code Context.Builder.withListener()} to hook into the
 * full bean-discovery and shutdown lifecycle.
 *
 * <p>Pre-scan callbacks ({@code beforeBeanDiscovery}, {@code processAnnotatedType},
 * {@code afterTypeDiscovery}) are invoked before any {@code @Observes} methods
 * are registered and therefore cannot be received via regular observer methods.</p>
 *
 * <p>Post-scan callbacks ({@code processObserverMethod}, {@code afterBeanDiscovery},
 * {@code afterDeploymentValidation}) are invoked before the corresponding event is
 * fired to {@code @Observes} methods, so listeners see the event first.</p>
 */
public interface ContainerListener {

    default void beforeBeanDiscovery(BeforeBeanDiscovery event) {}

    default void processAnnotatedType(ProcessAnnotatedType event) {}

    default void afterTypeDiscovery(AfterTypeDiscovery event) {}

    default void processObserverMethod(ProcessObserverMethod event) {}

    default void afterBeanDiscovery(AfterBeanDiscovery event) {}

    default void afterDeploymentValidation(AfterDeploymentValidation event) {}

    default void beforeShutdown(BeforeShutdown event) {}
}
