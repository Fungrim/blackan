package io.github.fungrim.blackan.bootstrap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import io.github.fungrim.blackan.bootstrap.classloader.RuntimeClassLoader;
import io.github.fungrim.blackan.bootstrap.layout.ApplicationLayout;

public class BlackanBootstrap {

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, Throwable {
        // create application layout
        ApplicationLayout layout = ApplicationLayout.builder().build();
        System.out.println("BlackanBootstrap.main: ");
        System.out.println(" - layout: " + layout);
        // create runtime loader
        RuntimeClassLoader loader = RuntimeClassLoader.builder()
                .withLayout(layout)
                .build();
        // get runtime class 
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> runtimeClass = loader.loadClass(layout.getRuntimeClassName());
        System.out.println("BlackanBootstrap.main: ");
        System.out.println(" - runtimeClass: " + runtimeClass);
        // go!
        MethodHandles
                .lookup()
                .findStatic(runtimeClass, "main", MethodType.methodType(void.class, String[].class))
                .invokeExact(args);
    }
}
