plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.3.0")
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("org.jetbrains.gradle.plugin.idea-ext:org.jetbrains.gradle.plugin.idea-ext.gradle.plugin:1.1.10")
}
