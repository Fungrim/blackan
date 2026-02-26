package io.github.fungrim.blackan.injector.creator.stubs;

import jakarta.inject.Inject;

public class FullInjectedBean {

    @Inject
    public Integer count;

    public String name;
    public Long id;

    @Inject
    public FullInjectedBean(String name) {
        this.name = name;
    }

    @Inject
    public void init(Long id) {
        this.id = id;
    }
}
