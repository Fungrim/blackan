package io.fungrim.github.blackan.gradle.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class ValidateDependenciesTask extends DefaultTask {

    @Input
    public abstract MapProperty<String, String> getBootArtifacts();

    @Input
    public abstract MapProperty<String, String> getRuntimeArtifacts();

    @Input
    public abstract MapProperty<String, String> getRuntimeClasspathArtifacts();

    @TaskAction
    public void validate() {
        Map<String, String> seen = new HashMap<>();
        Set<String> conflicts = new HashSet<>();
        check(getBootArtifacts().get(), seen, conflicts, "blackanBoot");
        check(getRuntimeArtifacts().get(), seen, conflicts, "blackanRuntime");
        check(getRuntimeClasspathArtifacts().get(), seen, conflicts, "runtimeClasspath");
        if (!conflicts.isEmpty()) {
            throw new GradleException("Dependency version conflicts detected:\n  " + String.join("\n  ", conflicts));
        }
    }

    private void check(Map<String, String> artifacts, Map<String, String> seen,
                       Set<String> conflicts, String source) {
        for (Map.Entry<String, String> entry : artifacts.entrySet()) {
            String key = entry.getKey();
            String version = entry.getValue();
            String existing = seen.get(key);
            if (existing != null && !existing.equals(version)) {
                conflicts.add(key + " (" + existing + " vs " + version + ", detected in " + source + ")");
            } else {
                seen.put(key, version);
            }
        }
    }
}
