package io.fungrim.github.blackan.gradle;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

import io.fungrim.github.blackan.gradle.tasks.CopyBootTask;
import io.fungrim.github.blackan.gradle.tasks.CopyRuntimeTask;
import io.fungrim.github.blackan.gradle.tasks.CreateRunJarTask;
import io.fungrim.github.blackan.gradle.tasks.JandexTask;
import io.fungrim.github.blackan.gradle.tasks.ValidateDependenciesTask;
import io.fungrim.github.blackan.gradle.util.AppLibsReader;

public class BlackanPlugin implements Plugin<Project> {

    public void apply(Project project) {
        
        // register extension
        BlackanExtension extension = project.getExtensions().create("blackan", BlackanExtension.class);

        // create configurations
        Configuration bootConfig = createConfiguration(project, "blackanBoot");
        Configuration blackanRuntime = createConfiguration(project, "blackanRuntime");

        // read app libs
        AppLibsReader.AppLibs appLibs = AppLibsReader.fromResources();
        appLibs.boot().forEach(gav -> project.getDependencies().add("blackanBoot", gav));
        appLibs.runtime().forEach(gav -> project.getDependencies().add("blackanRuntime", gav));

        // create directories
        Provider<Directory> appDir = project.getLayout().getBuildDirectory().dir("blackan-app");
        Provider<Directory> bootDir = appDir.map(d -> d.dir("boot"));
        Provider<Directory> runtimeDir = appDir.map(d -> d.dir("runtime"));

        // register jandex task
        registerJandexTask(project, extension);

        // register validate dependencies task
        project.getTasks().register("blackanValidateDependencies", ValidateDependenciesTask.class, task -> {
            task.getBootArtifacts().set(project.provider(
                    () -> resolveArtifactMap(bootConfig)));
            task.getRuntimeArtifacts().set(project.provider(
                    () -> resolveArtifactMap(blackanRuntime)));
            task.getRuntimeClasspathArtifacts().set(project.provider(
                    () -> resolveArtifactMap(project.getConfigurations().getByName("runtimeClasspath"))));
        });

        // register copy boot task
        project.getTasks().register("blackanCopyBoot", CopyBootTask.class, task -> {
            task.dependsOn("blackanValidateDependencies");
            task.getBootFiles().from(bootConfig);
            task.getBootDirectory().set(bootDir);
        });

        // register copy runtime task
        project.getTasks().register("blackanCopyRuntime", CopyRuntimeTask.class, task -> {
            task.dependsOn("blackanValidateDependencies", "jar");
            task.getRuntimeClasspathFiles().from(project.getConfigurations().getByName("runtimeClasspath"));
            task.getBlackanRuntimeFiles().from(blackanRuntime);
            task.getBootFiles().from(bootConfig);
            task.getProjectJar().set(
                    ((Jar) project.getTasks().getByName("jar")).getArchiveFile());
            task.getRuntimeDirectory().set(runtimeDir);
        });

        // register create run jar task
        project.getTasks().register("blackanCreateRunJar", CreateRunJarTask.class, task -> {
            task.dependsOn("blackanCopyBoot");
            task.getDestinationDirectory().set(appDir);
            task.getBootFiles().from(bootConfig);
            task.doFirst(t -> ((CreateRunJarTask) t).configureClassPath());
        });

        // register assemble app task
        project.getTasks().register("blackanAssembleApp", task -> {
            task.dependsOn("blackanCopyBoot", "blackanCopyRuntime", "blackanCreateRunJar");
            task.setGroup("build");
            task.setDescription("Assembles the Blackan application directory");
        });

        // register assemble task
        project.afterEvaluate(p ->
            p.getTasks().named("assemble").configure(task ->
                task.dependsOn("blackanAssembleApp")
            )
        );
    }

    private static Map<String, String> resolveArtifactMap(Configuration config) {
        Map<String, String> map = new HashMap<>();
        for (ResolvedArtifact artifact : config.getResolvedConfiguration().getResolvedArtifacts()) {
            String key = artifact.getModuleVersion().getId().getGroup()
                    + ":" + artifact.getModuleVersion().getId().getName();
            map.put(key, artifact.getModuleVersion().getId().getVersion());
        }
        return map;
    }

    private Configuration createConfiguration(Project project, String name) {
        return project.getConfigurations().create(name, c -> {
            c.setTransitive(true);
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
        });
    }

    private void registerJandexTask(Project project, BlackanExtension extension) {
        Provider<Directory> jandexOutputDir = project.getLayout().getBuildDirectory().dir("generated/resources/jandex");

        Configuration directDeps = project.getConfigurations().create("blackanJandexDeps", c -> {
            c.setTransitive(false);
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
        });

        project.afterEvaluate(p -> {
            Configuration implConfig = p.getConfigurations().findByName("implementation");
            if (implConfig != null) {
                implConfig.getDependencies().forEach(dep ->
                        directDeps.getDependencies().add(dep.copy()));
            }
            Configuration apiConfig = p.getConfigurations().findByName("api");
            if (apiConfig != null) {
                apiConfig.getDependencies().forEach(dep ->
                        directDeps.getDependencies().add(dep.copy()));
            }
        });

        project.getTasks().register("blackanJandex", JandexTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Generates a Jandex index for the service");
            task.dependsOn("compileJava");

            JavaPluginExtension javaExt = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaExt.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            task.getClassesDirs().from(mainSourceSet.getOutput().getClassesDirs());
            task.getDependencyFiles().from(directDeps);

            task.getIndexAll().set(extension.getIndexAll());
            task.getIncludes().set(extension.getIncludes());
            task.getExcludes().set(extension.getExcludes());
            task.getOutputDirectory().set(jandexOutputDir);
        });

        project.afterEvaluate(p -> {
            JavaPluginExtension javaExt = p.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaExt.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            mainSourceSet.getResources().srcDir(jandexOutputDir);
            p.getTasks().named("processResources").configure(t -> t.dependsOn("blackanJandex"));
        });
    }
}