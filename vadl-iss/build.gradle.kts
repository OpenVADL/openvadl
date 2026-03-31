plugins {
    id("conventions-jvm")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))
    implementation(project(":vadl-core"))
    implementation(project(":vadl-pass-api"))
    implementation(project(":vadl-vdt"))
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(kotlin("stdlib-jdk8"))
}
