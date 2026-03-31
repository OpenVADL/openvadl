plugins {
    id("conventions-jvm")
    alias(libs.plugins.z3)
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))
    implementation(project(":vadl-core"))
    implementation(project(":vadl-pass-api"))
    implementation(libs.commons.lang3)
    implementation(libs.z3.bootstrap)
}
