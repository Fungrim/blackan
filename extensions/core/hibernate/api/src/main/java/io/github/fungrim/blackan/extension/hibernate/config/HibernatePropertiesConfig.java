package io.github.fungrim.blackan.extension.hibernate.config;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "hibernate")
public interface HibernatePropertiesConfig {

    @WithName("dialect")
    Optional<String> dialect();

    @WithName("show_sql")
    @WithDefault("false")
    boolean showSql();

    @WithName("format_sql")
    @WithDefault("false")
    boolean formatSql();

    @WithName("use_sql_comments")
    @WithDefault("false")
    boolean useSqlComments();

    @WithName("hbm2ddl.auto")
    Optional<String> hbm2ddlAuto();

    @WithName("default_schema")
    Optional<String> defaultSchema();

    @WithName("default_catalog")
    Optional<String> defaultCatalog();

    @WithName("connection.autocommit")
    @WithDefault("false")
    boolean connectionAutocommit();

    @WithName("connection.pool_size")
    Optional<Integer> connectionPoolSize();

    @WithName("current_session_context_class")
    Optional<String> currentSessionContextClass();

    @WithDefault("0")
    @WithName("jdbc.batch_size")
    int jdbcBatchSize();

    @WithName("jdbc.fetch_size")
    Optional<Integer> jdbcFetchSize();

    @WithDefault("false")
    @WithName("order_inserts")
    boolean orderInserts();

    @WithDefault("false")
    @WithName("order_updates")
    boolean orderUpdates();

    @WithDefault("false")
    @WithName("generate_statistics")
    boolean generateStatistics();

    @WithDefault("false")
    @WithName("cache.use_second_level_cache")
    boolean cacheUseSecondLevelCache();

    @WithDefault("false")
    @WithName("cache.use_query_cache")
    boolean cacheUseQueryCache();

    @WithName("cache.region.factory_class")
    Optional<String> cacheRegionFactoryClass();

    @WithDefault("true")
    @WithName("jpa.compliance.query")
    boolean jpaComplianceQuery();

    @WithName("jpa.compliance.transaction")
    @WithDefault("true")
    boolean jpaComplianceTransaction();

    @WithDefault("true")
    @WithName("jpa.compliance.closed")
    boolean jpaComplianceClosed();

    @WithDefault("true")
    @WithName("jpa.compliance.proxy")
    boolean jpaComplianceProxy();

    @WithDefault("true")
    @WithName("jpa.compliance.caching")
    boolean jpaComplianceCaching();

    default void applyTo(org.hibernate.cfg.Configuration configuration) {
        dialect().ifPresent(value -> 
            configuration.setProperty("hibernate.dialect", value));
        configuration.setProperty("hibernate.show_sql", String.valueOf(showSql()));
        configuration.setProperty("hibernate.format_sql", String.valueOf(formatSql()));
        configuration.setProperty("hibernate.use_sql_comments", String.valueOf(useSqlComments()));
        hbm2ddlAuto().ifPresent(value -> 
            configuration.setProperty("hibernate.hbm2ddl.auto", value));
        defaultSchema().ifPresent(value -> 
            configuration.setProperty("hibernate.default_schema", value));
        defaultCatalog().ifPresent(value -> 
            configuration.setProperty("hibernate.default_catalog", value));
        configuration.setProperty("hibernate.connection.autocommit", 
            String.valueOf(connectionAutocommit()));
        connectionPoolSize().ifPresent(value -> 
            configuration.setProperty("hibernate.connection.pool_size", String.valueOf(value)));
        currentSessionContextClass().ifPresent(value -> 
            configuration.setProperty("hibernate.current_session_context_class", value));
        configuration.setProperty("hibernate.jdbc.batch_size", String.valueOf(jdbcBatchSize()));
        jdbcFetchSize().ifPresent(value -> 
            configuration.setProperty("hibernate.jdbc.fetch_size", String.valueOf(value)));
        configuration.setProperty("hibernate.order_inserts", String.valueOf(orderInserts()));
        configuration.setProperty("hibernate.order_updates", String.valueOf(orderUpdates()));
        configuration.setProperty("hibernate.generate_statistics", 
            String.valueOf(generateStatistics()));
        configuration.setProperty("hibernate.cache.use_second_level_cache", 
            String.valueOf(cacheUseSecondLevelCache()));
        configuration.setProperty("hibernate.cache.use_query_cache", 
            String.valueOf(cacheUseQueryCache()));
        cacheRegionFactoryClass().ifPresent(value -> 
            configuration.setProperty("hibernate.cache.region.factory_class", value));
        configuration.setProperty("hibernate.jpa.compliance.query", 
            String.valueOf(jpaComplianceQuery()));
        configuration.setProperty("hibernate.jpa.compliance.transaction", 
            String.valueOf(jpaComplianceTransaction()));
        configuration.setProperty("hibernate.jpa.compliance.closed", 
            String.valueOf(jpaComplianceClosed()));
        configuration.setProperty("hibernate.jpa.compliance.proxy", 
            String.valueOf(jpaComplianceProxy()));
        configuration.setProperty("hibernate.jpa.compliance.caching", 
            String.valueOf(jpaComplianceCaching()));
    }
}
