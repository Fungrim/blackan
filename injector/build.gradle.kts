plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jakarta.inject)
    api(libs.cdi.spec)
    api(libs.validation.api)
    api(libs.jandex)
    implementation(project(":common"))
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
