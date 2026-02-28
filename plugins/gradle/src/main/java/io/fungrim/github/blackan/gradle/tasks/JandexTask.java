package io.fungrim.github.blackan.gradle.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

import io.fungrim.github.blackan.gradle.util.DependencyMatcher;

public abstract class JandexTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getClassesDirs();

    @InputFiles
    public abstract ConfigurableFileCollection getDependencyFiles();

    @Input
    public abstract Property<Boolean> getIndexAll();

    @Input
    public abstract ListProperty<String> getIncludes();

    @Input
    public abstract ListProperty<String> getExcludes();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generate() throws IOException {
        Indexer indexer = new Indexer();

        indexClassesDirs(indexer);
        indexDependencies(indexer);

        Index index = indexer.complete();
        getLogger().info("Jandex index created with {} classes", index.getKnownClasses().size());
        for (ClassInfo ci : index.getKnownClasses()) {
            getLogger().debug("  - {}", ci.name());
        }

        File metaInf = new File(getOutputDirectory().get().getAsFile(), "META-INF");
        metaInf.mkdirs();
        File indexFile = new File(metaInf, "jandex.idx");
        try (FileOutputStream out = new FileOutputStream(indexFile)) {
            new IndexWriter(out).write(index);
        }
    }

    private void indexClassesDirs(Indexer indexer) throws IOException {
        for (File dir : getClassesDirs()) {
            if (dir.exists() && dir.isDirectory()) {
                indexDirectory(indexer, dir, dir);
            }
        }
    }

    private void indexDirectory(Indexer indexer, File root, File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                indexDirectory(indexer, root, file);
            } else if (file.getName().endsWith(".class")) {
                try (InputStream is = file.toURI().toURL().openStream()) {
                    indexer.index(is);
                }
            }
        }
    }

    private void indexDependencies(Indexer indexer) throws IOException {
        boolean indexAll = getIndexAll().get();
        List<String> includes = getIncludes().get();
        List<String> excludes = getExcludes().get();

        // nothing to do here
        if (!indexAll && includes.isEmpty()) {
            getLogger().info("No dependencies to index");
            return;
        }

        for (File file : getDependencyFiles()) {
            if (!file.exists() || !file.getName().endsWith(".jar")) {
                continue;
            }
            String coordinate = DependencyMatcher.extractCoordinate(file);
            if (coordinate == null) {
                continue;
            }
            if (DependencyMatcher.shouldIndex(coordinate, indexAll, includes, excludes)) {
                indexJar(indexer, file);
            }
        }
    }

    private void indexJar(Indexer indexer, File jarFile) throws IOException {
        getLogger().info("Indexing JAR: {}", jarFile.getName());
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        indexer.index(is);
                    }
                }
            }
        }
    }
}
