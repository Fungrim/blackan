package io.github.fungrim.blackan.extension.jetty;

import java.util.List;

public interface RestContextConfig {

    String getContextPath();

    List<String> getVirtualHosts();

}
