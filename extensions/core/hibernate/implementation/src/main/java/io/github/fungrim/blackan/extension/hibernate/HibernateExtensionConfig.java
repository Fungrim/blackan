package io.github.fungrim.blackan.extension.hibernate;

import java.util.List;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.whatap.grok.api.Grok;

@ConfigMapping(prefix = "blackan.hibernate.discovery")
public interface HibernateExtensionConfig {

    @WithName("whitelist")
    List<Grok> whitelist();

    @WithName("blacklist")
    List<Grok> blacklist();
    
}
