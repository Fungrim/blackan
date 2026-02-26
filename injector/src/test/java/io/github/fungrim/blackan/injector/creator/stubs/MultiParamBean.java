package io.github.fungrim.blackan.injector.creator.stubs;

import jakarta.inject.Inject;

public class MultiParamBean {

    public final String name;
    public final Integer count;

    @Inject
    public MultiParamBean(String name, Integer count) {
        this.name = name;
        this.count = count;
    }
}
