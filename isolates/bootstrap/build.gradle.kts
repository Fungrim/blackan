plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.classgraph)
    // test classes
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
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

publishing {
    publications {
        create<MavenPublication>("bootstrap") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}