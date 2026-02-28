package io.github.fungrim.blackan.runtime;

import java.io.IOException;

import org.jboss.jandex.IndexView;

import io.github.fungrim.blackan.bootstrap.classloader.RuntimeClassLoader;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.runtime.jandex.CompositeIndexViewFactory;
import io.github.fungrim.blackan.runtime.util.StageAndPriorityComparator;

public class RuntimeBootstrap {

    public static void main(String[] args) throws IOException, InterruptedException {
        if(RuntimeBootstrap.class.getClassLoader() instanceof RuntimeClassLoader loader) {
            System.out.println("RuntimeBootstrap.main.jars: ");
            loader.getApplicationJars().forEach(j -> System.out.println(" - " + j));
            IndexView indexView = new CompositeIndexViewFactory(loader.getApplicationJars()).create();
            System.out.println("RuntimeBootstrap.index.classes: ");
            indexView.getKnownClasses().forEach(c -> System.out.println(" - " + c.name().toString()));
            Context context = Context.builder()
                .withIndex(indexView)
                .withEventOrdering(new StageAndPriorityComparator())
                .build();
            new RuntimeController(context).run();
        } else {
            throw new IllegalStateException("RuntimeBootstrap.main: class loader is not an instance of RuntimeClassLoader: " + RuntimeBootstrap.class.getClassLoader().getClass().getName());
        }
    }
}
