package io.github.fungrim.blackan.injector.util.stubs;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class GenericInjectionBean {

    @Inject
    public List<String> stringList;

    @Inject
    public List<Integer> integerList;

    @Inject
    @Named("production-list")
    public List<Integer> productionIntegerList;

    @Inject
    @Named("greeting-list")
    public List<String> greetingList;
}
