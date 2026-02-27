plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(libs.jandex)
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

gradlePlugin {
    plugins {
        create("blackan") {
            id = "io.fungrim.github.blackan.gradle"
            implementationClass = "io.fungrim.github.blackan.gradle.BlackanPlugin"
        }
    }
}

version = (rootProject.extensions.findByName("semver") as net.thauvin.erik.gradle.semver.SemverConfig).version
group = "io.github.fungrim.blackan.plugins"

publishing {
    repositories {
        mavenLocal()
    }
}