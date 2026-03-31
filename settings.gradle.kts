pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.3.0"
        id("me.qoomon.git-versioning") version "6.4.4"
        id("io.github.rascmatt.z3") version "1.0.2"
        id("org.graalvm.buildtools.native") version "0.11.2"
        id("org.beryx.jlink") version "3.2.0"
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
include("java-annotations")
include("vadl-cli")
include("vadl-lsp")
include("vadl-test")
