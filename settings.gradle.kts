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
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "open-vadl"

val localKlspDir = startParameter.projectProperties["localKlspDir"] ?: System.getenv("KLSP_DIR")
if (localKlspDir != null) {
    includeBuild(localKlspDir)
}

include("vadl")
include("java-annotations")
include("vadl-cli")
include("vadl-lsp")
include("vadl-test")
