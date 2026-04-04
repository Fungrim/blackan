package io.github.fungrim.blackan.runtime;

import java.io.IOException;

import org.jboss.jandex.IndexView;

import io.github.fungrim.blackan.bootstrap.BootstrapLogger;
import io.github.fungrim.blackan.bootstrap.classloader.RuntimeClassLoader;
import io.github.fungrim.blackan.injector.Context;
import io.github.fungrim.blackan.runtime.jandex.CompositeIndexViewFactory;
import io.github.fungrim.blackan.runtime.util.StageAndPriorityComparator;

public class RuntimeBootstrap {

    public static void main(String[] args) throws IOException, InterruptedException {
        if(RuntimeBootstrap.class.getClassLoader() instanceof RuntimeClassLoader loader) {
            BootstrapLogger logger = BootstrapLogger.of(args);
            logger.println("RuntimeBootstrap.main.jars: ");
            loader.getApplicationJars().forEach(j -> logger.println(" - " + j));
            IndexView indexView = new CompositeIndexViewFactory(loader.getApplicationJars()).create();
            logger.println("RuntimeBootstrap.index.classes: ");
            indexView.getKnownClasses().forEach(c -> logger.println(" - " + c.name().toString()));
            Context context = Context.builder()
                .withIndex(indexView)
                .withCustomEventOrdering(new StageAndPriorityComparator())
                .build();
            logger.println("RuntimeBootstrap.context.producers: ");
            context.producerRegistry().keys().forEach(k -> logger.println(" - " + k.toString()));
            new RuntimeController(context).run();
        } else {
            throw new IllegalStateException("RuntimeBootstrap.main: class loader is not an instance of RuntimeClassLoader: " + RuntimeBootstrap.class.getClassLoader().getClass().getName());
        }
    }
}
