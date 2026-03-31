plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl"))
    implementation(libs.lsp4j)

    testImplementation(libs.assertj.core)
}
