plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl-core"))
    implementation(libs.thymeleaf)
    implementation(libs.commons.lang3)
}
