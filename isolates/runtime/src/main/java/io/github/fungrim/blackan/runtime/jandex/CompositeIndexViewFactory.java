package io.github.fungrim.blackan.runtime.jandex;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class CompositeIndexViewFactory {

    private final List<URL> applicationJars;

    public IndexView create() {
        List<IndexView> indexes = new ArrayList<>();
        System.out.println("CompositeIndexViewFactory.create: ");
        try (ScanResult scanResult = createGraph().scan()) {
            scanResult.getResourcesWithPath("META-INF/jandex.idx")
                .stream()
                .map(this::createIndex)
                .forEach(indexes::add);
        }
        return CompositeIndex.create(indexes);
    }

    private ClassGraph createGraph() {
        URL[] urls = applicationJars.toArray(new URL[applicationJars.size()]);
        return new ClassGraph()
            .overrideClassLoaders(new URLClassLoader(urls))
            .ignoreParentClassLoaders()
            .disableDirScanning()
            .enableExternalClasses()
            .verbose(false);
    }

    private IndexView createIndex(Resource resource) {
        try {
            System.out.println(" - resource: " + resource);
            IndexReader reader = new IndexReader(resource.open());
            return reader.read();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read index from resource: " + resource, e);
        }
    }
}
