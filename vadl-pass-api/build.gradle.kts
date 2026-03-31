plugins {
    id("conventions-jvm")
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))
    implementation(project(":vadl-core"))
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(libs.thymeleaf)
    implementation(libs.commons.lang3)
}
