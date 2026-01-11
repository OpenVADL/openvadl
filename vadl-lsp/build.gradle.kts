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
    // Add logback modules for SLF4J logging and java.naming required by logback
    addOptions("--add-modules", "ch.qos.logback.classic,ch.qos.logback.core,java.naming")

    forceMerge(".*")

    launcher {
        name = "openvadl-lsp"
        jvmArgs = listOf("-Dslf4j.internal.verbosity=WARN")
    }

    mergedModule {
        excludeProvides(mapOf("service" to "jakarta.servlet.ServletContainerInitializer"))
    }

    moduleName.set("openvadl.lsp")
    mergedModuleName.set("openvadl.lsp")
    mainClass.set("vadl.lsp.Main")
}

// The plugin only merges dependency jars, not the main application jar.
// Manually copy it into mergedjars to include it in the merged module.
tasks.named("prepareMergedJarsDir") {
    doLast {
        copy {
            from(zipTree(tasks.jar.get().archiveFile))
            into("${layout.buildDirectory.get()}/jlinkbase/mergedjars")
        }
    }
}
