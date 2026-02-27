plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":injector"))
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
