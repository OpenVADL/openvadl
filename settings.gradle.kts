pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    includeBuild("gradle-conventions")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "open-vadl"

include("vadl")
include("vadl-core")
include("vadl-pass-api")
include("java-annotations")
include("vadl-cli")
include("vadl-lsp")
include("vadl-test")
