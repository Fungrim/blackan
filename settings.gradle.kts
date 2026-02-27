plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "blackan"
include("common", "injector", "isolates:bootstrap", "isolates:runtime", "isolates:testservice", "isolates:testjar", "plugins:gradle", "extensions:bootstrap:slf4j")