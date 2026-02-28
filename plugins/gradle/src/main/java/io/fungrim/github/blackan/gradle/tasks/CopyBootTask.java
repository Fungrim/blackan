package io.fungrim.github.blackan.gradle.tasks;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public abstract class CopyBootTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getBootFiles();

    @OutputDirectory
    public abstract DirectoryProperty getBootDirectory();

    @Inject
    protected abstract FileSystemOperations getFs();

    @TaskAction
    public void copy() {
        getFs().sync(spec -> {
            spec.from(getBootFiles());
            spec.into(getBootDirectory());
        });
    }
}
