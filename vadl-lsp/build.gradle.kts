plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl"))
    implementation(project(":vadl-frontend"))
    implementation(libs.lsp4j)

    testImplementation(libs.assertj.core)
    testCompileOnly(libs.jsr305)
    testImplementation(libs.guava)
}
