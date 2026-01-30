plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.gradle.plugin.idea-ext:org.jetbrains.gradle.plugin.idea-ext.gradle.plugin:1.1.10")
    implementation(libs.errorprone.plugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

