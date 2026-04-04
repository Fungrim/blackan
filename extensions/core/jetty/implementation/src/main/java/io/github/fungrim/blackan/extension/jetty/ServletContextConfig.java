package io.github.fungrim.blackan.extension.jetty;

import java.util.List;

public interface ServletContextConfig {

    String getContextPath();

    List<String> getVirtualHosts();
    
}
