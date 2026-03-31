plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")

    testImplementation("org.assertj:assertj-core:3.26.3")
}
