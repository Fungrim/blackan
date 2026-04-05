package io.github.fungrim.blackan.extension.hibernate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.extension.grok.GrokExtension;
import io.github.fungrim.blackan.extension.hibernate.config.HibernatePropertiesConfig;
import io.github.fungrim.blackan.extension.hibernate.config.JakartaPersistenceConfig;
import io.github.fungrim.blackan.extension.hibernate.dao.BlacklistedEntity;
import io.github.fungrim.blackan.extension.hibernate.dao.NormalEntity;
import io.github.fungrim.blackan.extension.hibernate.dao.WhitelistedEntity;
import io.github.fungrim.blackan.extension.slf4j.Slf4jExtension;
import io.github.fungrim.blackan.extension.smallrye.config.SmallRyeConfigExtension;
import io.github.fungrim.blackan.injector.Context;
import jakarta.inject.Inject;

class HibernateExtensionTest {

    private Context ctx;

    @Inject
    SessionFactory sessionFactory;

    @BeforeEach
    public void setup() throws IOException {
        this.ctx = Context.builder()
                .withClasses(List.of(
                    Slf4jExtension.class,
                    SmallRyeConfigExtension.class,
                    GrokExtension.class,
                    HibernateExtension.class,
                    JakartaPersistenceConfig.class,
                    HibernatePropertiesConfig.class,
                    HibernateExtensionConfig.class,
                    WhitelistedEntity.class,
                    BlacklistedEntity.class,
                    NormalEntity.class))
                .build();
        this.ctx.fireInCustomOrder(new RuntimeStartEvent());
        this.ctx.decorate(this);
    }

    @Test
    void whitelistedEntityIsIncluded() {
        MappingMetamodelImplementor metamodel = (MappingMetamodelImplementor) sessionFactory.getMetamodel();
        assertDoesNotThrow(() -> metamodel.entity(WhitelistedEntity.class));
    }

    @Test
    void blacklistedEntityIsExcluded() {
        MappingMetamodelImplementor metamodel = (MappingMetamodelImplementor) sessionFactory.getMetamodel();
        assertThrows(IllegalArgumentException.class, () -> metamodel.entity(BlacklistedEntity.class));
    }

    @Test
    void normalEntityIsIncludedByDefault() {
        MappingMetamodelImplementor metamodel = (MappingMetamodelImplementor) sessionFactory.getMetamodel();
        assertDoesNotThrow(() -> metamodel.entity(NormalEntity.class));
    }
}
