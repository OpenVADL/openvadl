plugins {
    application
}

group = "vadl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":vadl"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")
    implementation("ch.qos.logback:logback-classic:1.5.13")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    applicationName = "openvadl-lsp"
    mainClass.set("vadl.lsp.Main")
}

// Tasks that should only be executed if the concrete path was specified (using the vadl-lsp: prefix)
var pathSpecificTasks = with(tasks) {
    setOf(run, installDist)
}.map { it.name }
tasks.filter { it.name in pathSpecificTasks }.forEach { it ->
    it.onlyIf {
        gradle.startParameter.taskNames.any { it.contains("vadl-lsp:") }
    }
}

tasks.test {
    useJUnitPlatform()
}