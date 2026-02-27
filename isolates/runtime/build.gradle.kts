plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":injector"))
    implementation(project(":isolates:bootstrap"))
    // test classes
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito)
    testImplementation(libs.jandex)
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