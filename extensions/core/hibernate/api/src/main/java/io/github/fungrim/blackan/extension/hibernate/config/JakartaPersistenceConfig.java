package io.github.fungrim.blackan.extension.hibernate.config;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "jakarta.persistence")
public interface JakartaPersistenceConfig {

    @WithName("jdbc.driver")
    Optional<String> jdbcDriver();

    @WithName("jdbc.url")
    Optional<String> jdbcUrl();

    @WithName("jdbc.user")
    Optional<String> jdbcUser();

    @WithName("jdbc.password")
    Optional<String> jdbcPassword();

    @WithName("schema-generation.database.action")
    Optional<String> schemaGenerationDatabaseAction();

    @WithName("schema-generation.create-source")
    Optional<String> schemaGenerationCreateSource();

    @WithName("schema-generation.drop-source")
    Optional<String> schemaGenerationDropSource();

    @WithName("schema-generation.create-script-source")
    Optional<String> schemaGenerationCreateScriptSource();

    @WithName("schema-generation.drop-script-source")
    Optional<String> schemaGenerationDropScriptSource();

    @WithName("schema-generation.scripts.action")
    Optional<String> schemaGenerationScriptsAction();

    @WithName("schema-generation.scripts.create-target")
    Optional<String> schemaGenerationScriptsCreateTarget();

    @WithName("schema-generation.scripts.drop-target")
    Optional<String> schemaGenerationScriptsDropTarget();

    @WithName("sql-load-script-source")
    Optional<String> sqlLoadScriptSource();

    @WithDefault("false")
    @WithName("create-database-schemas")
    boolean createDatabaseSchemas();

    default void applyTo(org.hibernate.cfg.Configuration configuration) {
        jdbcDriver().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.jdbc.driver", value));
        jdbcUrl().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.jdbc.url", value));
        jdbcUser().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.jdbc.user", value));
        jdbcPassword().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.jdbc.password", value));
        schemaGenerationDatabaseAction().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.database.action", value));
        schemaGenerationCreateSource().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.create-source", value));
        schemaGenerationDropSource().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.drop-source", value));
        schemaGenerationCreateScriptSource().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.create-script-source", value));
        schemaGenerationDropScriptSource().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.drop-script-source", value));
        schemaGenerationScriptsAction().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.scripts.action", value));
        schemaGenerationScriptsCreateTarget().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.scripts.create-target", value));
        schemaGenerationScriptsDropTarget().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.schema-generation.scripts.drop-target", value));
        sqlLoadScriptSource().ifPresent(value -> 
            configuration.setProperty("jakarta.persistence.sql-load-script-source", value));
        configuration.setProperty("jakarta.persistence.create-database-schemas", 
            String.valueOf(createDatabaseSchemas()));
    }
}
