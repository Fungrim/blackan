package io.github.fungrim.blackan.injector.util.stubs;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@ApplicationScoped
public class ListProducer {

    @Produces
    public List<String> stringList() {
        return List.of("alpha", "beta", "gamma");
    }

    @Produces
    public List<Integer> integerList() {
        return List.of(1, 2, 3);
    }

    @Produces
    @Named("production-list")
    public List<Integer> productionIntegerList() {
        return List.of(100, 200, 300);
    }

    @Produces
    @Named("greeting-list")
    public List<String> greetingList(Greeting greeting) {
        return List.of(greeting.greet(), greeting.greet().toUpperCase());
    }
}
