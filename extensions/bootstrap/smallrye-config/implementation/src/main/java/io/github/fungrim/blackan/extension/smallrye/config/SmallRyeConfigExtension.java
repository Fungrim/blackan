package io.github.fungrim.blackan.extension.smallrye.config;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.AfterBeanDiscovery;
import io.github.fungrim.blackan.common.cdi.AfterDeploymentValidation;
import io.github.fungrim.blackan.common.cdi.ContainerExtension;
import io.github.fungrim.blackan.common.cdi.InjectionPoint;
import io.github.fungrim.blackan.common.cdi.ProcessAnnotatedType;
import io.github.fungrim.blackan.common.cdi.TargetAwareProvider;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@Extension
@Priority(10)
@BootStage(Stage.BOOTSTRAP)
public class SmallRyeConfigExtension implements ContainerExtension {

    private static final DotName CONFIG_PROPERTY_NAME = DotName.createSimple(ConfigProperty.class.getName());
    private static final DotName CONFIG_MAPPING_NAME = DotName.createSimple(ConfigMapping.class.getName());

    private final List<RequiredProperty> requiredProperties = new ArrayList<>();
    private final List<DotName> configMappings = new ArrayList<>();
    
    private SmallRyeConfig config;

    record RequiredProperty(String propertyName, String className) {}

    // --- ContainerExtension lifecycle ---

    @Override
    public void processAnnotatedType(ProcessAnnotatedType event) {
        for (FieldInfo field : event.type().fields()) {
            AnnotationInstance ann = field.annotation(CONFIG_PROPERTY_NAME);
            if (ann == null) {
                continue;
            }
            AnnotationValue nameVal = ann.value("name");
            String propName = (nameVal != null && !nameVal.asString().isEmpty())
                    ? nameVal.asString()
                    : field.name();
            AnnotationValue defaultVal = ann.value("defaultValue");
            String defaultValue = defaultVal != null
                    ? defaultVal.asString()
                    : ConfigProperty.UNCONFIGURED_VALUE;
            if (ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue)) {
                requiredProperties.add(new RequiredProperty(propName, event.type().name().toString()));
            }
        }
        boolean hasConfigMapping = event.type().annotations().stream()
                .anyMatch(a -> a.name().equals(CONFIG_MAPPING_NAME)
                        && a.target() != null
                        && a.target().kind() == AnnotationTarget.Kind.CLASS);
        if (hasConfigMapping) {
            configMappings.add(event.type().name());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterBeanDiscovery(AfterBeanDiscovery event) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .addDiscoveredConverters();
        List<Class<?>> mappingClasses = new ArrayList<>();
        for (DotName name : configMappings) {
            try {
                Class<?> mappingClass = cl.loadClass(name.toString());
                builder.withMapping(mappingClass);
                mappingClasses.add(mappingClass);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load @ConfigMapping class: " + name, e);
            }
        }
        config = builder.build();
        event.addBean(SmallRyeConfig.class, ApplicationScoped.class, () -> config);
        for (Class<?> mc : mappingClasses) {
            event.addBean((Class<Object>) mc, ApplicationScoped.class,
                    () -> config.getConfigMapping(mc));
        }
    }

    @Override
    public void afterDeploymentValidation(AfterDeploymentValidation event) {
        for (RequiredProperty rp : requiredProperties) {
            if (config.getOptionalValue(rp.propertyName(), String.class).isEmpty()) {
                event.addDeploymentProblem(new IllegalStateException(
                        "Missing required config property '" + rp.propertyName()
                        + "' required by " + rp.className()));
            }
        }
    }

    // --- CDI @Produces for @ConfigProperty injection ---

    @Produces
    @ApplicationScoped
    @ConfigProperty
    public TargetAwareProvider<ConfigValue> produceConfigValueProperty(SmallRyeConfig config) {
        return (target, isOptional) -> config.getConfigValue(resolvePropertyName(target));
    }

    @Produces
    @ApplicationScoped
    @ConfigProperty
    public TargetAwareProvider<String> produceStringConfigProperty(SmallRyeConfig config) {
        return (target, isOptional) -> getConfigValue(target, config, String.class);
    }

    @Produces
    @ApplicationScoped
    @ConfigProperty
    public TargetAwareProvider<Integer> produceIntegerConfigProperty(SmallRyeConfig config) {
        return (target, isOptional) -> getConfigValue(target, config, Integer.class);
    }

    @Produces
    @ApplicationScoped
    @ConfigProperty
    public TargetAwareProvider<Long> produceLongConfigProperty(SmallRyeConfig config) {
        return (target, isOptional) -> getConfigValue(target, config, Long.class);
    }

    @Produces
    @ApplicationScoped
    @ConfigProperty
    public TargetAwareProvider<Boolean> produceBooleanConfigProperty(SmallRyeConfig config) {
        return (target, isOptional) -> getConfigValue(target, config, Boolean.class);
    }

    @Produces
    @ApplicationScoped
    @ConfigProperty
    public TargetAwareProvider<Double> produceDoubleConfigProperty(SmallRyeConfig config) {
        return (target, isOptional) -> getConfigValue(target, config, Double.class);
    }

    // --- Helpers ---

    private static <T> T getConfigValue(InjectionPoint target, SmallRyeConfig config, Class<T> type) {
        String name = resolvePropertyName(target);
        String defaultValue = resolveDefaultValue(target);
        if (defaultValue != null) {
            return config.getOptionalValue(name, type).orElseGet(() -> config.convert(defaultValue, type));
        }
        return config.getValue(name, type);
    }

    private static String resolvePropertyName(InjectionPoint target) {
        for (Annotation a : target.qualifiers()) {
            if (a instanceof ConfigProperty cp && !cp.name().isEmpty()) {
                return cp.name();
            }
        }
        return target.targetName();
    }

    private static String resolveDefaultValue(InjectionPoint target) {
        for (Annotation a : target.qualifiers()) {
            if (a instanceof ConfigProperty cp) {
                String dv = cp.defaultValue();
                return ConfigProperty.UNCONFIGURED_VALUE.equals(dv) ? null : dv;
            }
        }
        return null;
    }
}
