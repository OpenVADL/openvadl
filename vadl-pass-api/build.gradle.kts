plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl-core"))
    implementation(libs.commons.lang3)
}
