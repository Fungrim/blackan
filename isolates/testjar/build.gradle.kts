plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

val boot: Configuration by configurations.creating {
    isTransitive = true
}

val runtime: Configuration by configurations.creating {
    isTransitive = true
}

dependencies {
    boot(project(":isolates:bootstrap"))
    runtime(project(":isolates:runtime"))
    runtime(project(":isolates:testservice"))
    runtime(project(":extensions:bootstrap:slf4j"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

version = (rootProject.extensions.findByName("semver") as net.thauvin.erik.gradle.semver.SemverConfig).version
group = "io.github.fungrim.blackan.isolates"

val appDir = layout.buildDirectory.dir("blackan-app")
val bootDir = appDir.map { it.dir("boot") }
val runtimeDir = appDir.map { it.dir("runtime") }

val copyBoot by tasks.registering(Copy::class) {
    from(boot)
    into(bootDir)
    outputs.upToDateWhen { false }
}

val copyRuntime by tasks.registering(Copy::class) {
    from(runtime.minus(boot))
    into(runtimeDir)
    outputs.upToDateWhen { false }
}

val createRunJar by tasks.registering(Jar::class) {
    dependsOn(copyBoot)
    archiveFileName.set("blackan-run.jar")
    destinationDirectory.set(appDir)
    outputs.upToDateWhen { false }
    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Main-Class" to "io.github.fungrim.blackan.bootstrap.BlackanBootstrap",
            "Class-Path" to boot.joinToString(" ") { "boot/${it.name}" },
            "Add-Opens" to "java.base/java.lang"
        )
    }
}

val assembleApp by tasks.registering(Zip::class) {
    dependsOn(copyBoot, copyRuntime, createRunJar)
    from(appDir)
    archiveFileName.set("blackan-app.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    outputs.upToDateWhen { false }
}

tasks.named("assemble") {
    dependsOn(assembleApp)
}

publishing {
    publications {
        create<MavenPublication>("testjar") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(assembleApp) {
                extension = "jar"
            }
        }
    }
    repositories {
        mavenLocal()
    }
}