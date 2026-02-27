plugins {
    `java-library`
    `maven-publish`
    id("org.kordamp.gradle.jandex") version "2.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jakarta.inject)
    api(libs.cdi.spec)
    // test classes
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // lombok
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
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
group = "io.github.fungrim.blackan"

publishing {
    publications {
        create<MavenPublication>("common") {
            from(components["java"])       
        }
    }
    repositories {
        mavenLocal()
    }
}