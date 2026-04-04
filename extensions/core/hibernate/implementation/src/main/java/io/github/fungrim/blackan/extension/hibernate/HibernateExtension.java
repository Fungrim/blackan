package io.github.fungrim.blackan.extension.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jboss.jandex.DotName;

import io.github.fungrim.blackan.common.api.BootStage;
import io.github.fungrim.blackan.common.api.Extension;
import io.github.fungrim.blackan.common.api.Stage;
import io.github.fungrim.blackan.common.cdi.RuntimeStartEvent;
import io.github.fungrim.blackan.extension.hibernate.config.HibernatePropertiesConfig;
import io.github.fungrim.blackan.extension.hibernate.config.JakartaPersistenceConfig;
import io.github.fungrim.blackan.injector.Context;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.Entity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Extension
@BootStage(Stage.CORE)
public class HibernateExtension {

    @Inject
    JakartaPersistenceConfig jakartaPersistenceConfig;
    
    @Inject
    HibernatePropertiesConfig hibernatePropertiesConfig;

    @Inject
    Context context;

    private SessionFactory sessionFactory;

    public void start(@Observes RuntimeStartEvent event) {
        log.info("Hibernate extension starting");
        this.sessionFactory = createSessionFactory();
    }

    @Default
    @Produces
    @Singleton
    public SessionFactory sessionFactory() {
        return this.sessionFactory;
    }
    
    private SessionFactory createSessionFactory() {
        Configuration conf = new Configuration();
        jakartaPersistenceConfig.applyTo(conf);
        hibernatePropertiesConfig.applyTo(conf);
        log.debug("Scanning for entities... ");
        context.index().getAnnotations(DotName.createSimple(Entity.class)).forEach(annotation -> {
            log.debug("Adding entity {} to Hibernate configuration", annotation.target().asClass().name());
            conf.addAnnotatedClass(context.loadClass(annotation.target().asClass().name()));
        });
        log.debug("Building Hibernate SessionFactory");
        return conf.buildSessionFactory();
    }

    @Default
    @Produces
    @Singleton
    public TransactionOrchestrator transactionOrchestrator() {
        return null;
    }
}
