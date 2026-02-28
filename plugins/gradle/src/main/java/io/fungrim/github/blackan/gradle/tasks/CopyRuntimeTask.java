package io.fungrim.github.blackan.gradle.tasks;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public abstract class CopyRuntimeTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getRuntimeClasspathFiles();

    @InputFiles
    public abstract ConfigurableFileCollection getBlackanRuntimeFiles();

    @InputFiles
    public abstract ConfigurableFileCollection getBootFiles();

    @InputFile
    public abstract RegularFileProperty getProjectJar();

    @OutputDirectory
    public abstract DirectoryProperty getRuntimeDirectory();

    @Inject
    protected abstract FileSystemOperations getFs();

    @TaskAction
    public void copy() {
        Set<File> bootFileSet = getBootFiles().getFiles();
        File jar = getProjectJar().get().getAsFile();

        java.util.List<File> runtimeFiles = Stream.concat(
                        Stream.of(jar),
                        Stream.concat(
                                getRuntimeClasspathFiles().getFiles().stream(),
                                getBlackanRuntimeFiles().getFiles().stream()
                        ).distinct()
                )
                .filter(f -> !bootFileSet.contains(f))
                .distinct()
                .toList();

        getFs().sync(spec -> {
            spec.from(runtimeFiles);
            spec.into(getRuntimeDirectory());
        });
    }
}
