package io.github.fungrim.blackan.common.cdi;

/**
 * Callback interface for pre-scan container lifecycle events. Implement this
 * and register it via {@code Context.Builder.withListener()} to hook into
 * the bean-discovery phase before any {@code @Observes} methods have been
 * registered.
 */
public interface ContainerListener {

    default void beforeBeanDiscovery(BeforeBeanDiscovery event) {}

    default void processAnnotatedType(ProcessAnnotatedType event) {}

    default void afterTypeDiscovery(AfterTypeDiscovery event) {}
}
