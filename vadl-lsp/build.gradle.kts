
plugins {
    alias(libs.plugins.conventions.java)
}

dependencies {
    implementation(projects.vadl)
    implementation(libs.lsp4j)
}
