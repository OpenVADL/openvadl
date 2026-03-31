plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl-core"))
    implementation(project(":vadl-frontend"))
    implementation(libs.lsp4j)

    testImplementation(libs.assertj.core)
}
