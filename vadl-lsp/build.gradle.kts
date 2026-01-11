plugins {
    application
    id("org.beryx.jlink") version "3.2.0"
}

group = "vadl"
version = "unspecified"

repositories {
    mavenCentral()
}

configurations {
    implementation {
        exclude(group = "com.google.errorprone", module = "error_prone_core")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }
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

jlink {
    addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")

    launcher {
        name = "openvadl-lsp"
    }

    // Merge everything into one module
    forceMerge(".*")

    moduleName.set("openvadl.lsp")
    mergedModuleName.set("openvadl.lsp")
    mainClass.set("vadl.lsp.Main")
}

// Remove the problematic service provider declaration from logback
tasks.named("prepareMergedJarsDir") {
    doLast {
        delete("${layout.buildDirectory.get()}/jlinkbase/mergedjars/META-INF/services/jakarta.servlet.ServletContainerInitializer")

        // Copy vadl-lsp classes into mergedjars so they get included in the merged module
        copy {
            from(zipTree(tasks.jar.get().archiveFile))
            into("${layout.buildDirectory.get()}/jlinkbase/mergedjars")
        }
    }
}
