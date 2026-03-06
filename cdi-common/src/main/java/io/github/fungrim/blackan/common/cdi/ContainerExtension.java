package io.github.fungrim.blackan.common.cdi;

/**
 * Marker interface for Jandex-discoverable container extensions. Any
 * non-abstract class in the Jandex index that directly implements this
 * interface is automatically discovered and instantiated via its public
 * no-arg constructor during context initialisation, before any lifecycle
 * callbacks are fired.
 *
 * <p>Discovered instances receive the same lifecycle callbacks as a
 * programmatically registered {@link ContainerListener} via
 * {@code Context.Builder.withListener()}. If the same class is both
 * auto-discovered and registered programmatically, only the programmatically
 * registered instance is used.</p>
 *
 * <p>Implementations must have a public no-arg constructor. They are
 * instantiated before the container is ready and therefore do not support
 * dependency injection during lifecycle callbacks.</p>
 */
public interface ContainerExtension extends ContainerListener {
}
