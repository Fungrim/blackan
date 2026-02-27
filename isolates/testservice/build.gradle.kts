plugins {
    `java-library`
    `maven-publish`
    id("org.kordamp.gradle.jandex") version "2.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(libs.slf4j.api)
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
group = "io.github.fungrim.blackan.test"

publishing {
    publications {
        create<MavenPublication>("testservice") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}