plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl-core"))
    implementation(project(":vadl-frontend"))
    implementation(project(":vadl"))
    implementation(libs.lsp4j)

    testImplementation(libs.assertj.core)
}
