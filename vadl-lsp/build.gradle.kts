plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl"))
    implementation(libs.lsp4j)

    testImplementation(libs.assertj.core)
    testCompileOnly(libs.jsr305)
    testImplementation(libs.guava)
}
