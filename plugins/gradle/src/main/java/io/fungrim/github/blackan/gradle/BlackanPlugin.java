package io.fungrim.github.blackan.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BlackanPlugin implements Plugin<Project> {
    public void apply(Project project) {
        // register extensions
        project.getExtensions().create("blackanService", BlackanServicePluginExtension.class);

        // register tasks
        project.getTasks().register("buildBlackanService", task -> {
            
        });
    }
}