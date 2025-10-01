plugins {
    application
}

group = "vadl"
version = "unspecified"

dependencies {
    implementation(project(":vadl"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    applicationName = "openvadl-lsp"
    mainClass.set("vadl.lsp.Main")
}

tasks.test {
    useJUnitPlatform()
}