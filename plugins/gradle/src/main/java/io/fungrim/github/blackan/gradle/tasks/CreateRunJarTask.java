package io.fungrim.github.blackan.gradle.tasks;

import java.util.Map;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.bundling.Jar;

public abstract class CreateRunJarTask extends Jar {

    @InputFiles
    public abstract ConfigurableFileCollection getBootFiles();

    public CreateRunJarTask() {
        getArchiveFileName().set("blackan-run.jar");
        getOutputs().upToDateWhen(s -> false);
        manifest(manifest -> {
            manifest.attributes(Map.of(
                    "Manifest-Version", "1.0",
                    "Main-Class", "io.github.fungrim.blackan.bootstrap.BlackanBootstrap",
                    "Add-Opens", "java.base/java.lang"
            ));
        });
    }

    public void configureClassPath() {
        manifest(manifest ->
                manifest.getAttributes().put("Class-Path",
                        String.join(" ", getBootFiles().getFiles().stream()
                                .map(f -> "boot/" + f.getName())
                                .toList()))
        );
    }
}
