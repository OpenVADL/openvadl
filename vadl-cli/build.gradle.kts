plugins {
    id("conventions-jvm")
    application
    alias(libs.plugins.z3)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.jlink)
}

group = "vadl"
version = "unspecified"

dependencies {
    implementation(project(":vadl"))
    implementation(project(":vadl-lsp"))
    implementation(libs.picocli)
    implementation(libs.commons.compress)
    annotationProcessor(libs.picocli.codegen)
}

application {
    applicationName = "openvadl"
    mainClass.set("vadl.cli.Main")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

graalvmNative {
    binaries {
        fun org.graalvm.buildtools.gradle.dsl.NativeImageOptions.applyCommonConfig(gc: String) {
            resources {
                autodetection {
                    enabled = true
                    ignoreExistingResourcesConfigFile = true
                }
            }
            imageName.set("openvadl")
            mainClass.set(application.mainClass)
            buildArgs.addAll(
                "-O2",
                "--gc=$gc",
                "-R:MinHeapSize=4g",
                "-R:MaxNewSize=2g",
            )
            buildArgs.add("--enable-url-protocols=https")
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
        }

        named("main") {
            // The serial GC is different from the GC as for graalvm builds, which is mainly because this is the
            // only one that also works outside Linux.
            applyCommonConfig("serial")
        }
        create("epsilon") {
            // Epsilon is a no-op GC: memory is never reclaimed. Use it for short-lived runs where GC overhead is
            // undesirable. Produced via the `nativeEpsilonCompile` task.
            // The plugin only auto-wires the classpath for the `main` and `test` binaries, so we have to wire the
            // main source set's runtime classpath ourselves.
            classpath.from(sourceSets["main"].runtimeClasspath)
            applyCommonConfig("epsilon")
        }
    }
}

tasks.startScripts {
    defaultJvmOpts = listOf(
        "-XX:TieredStopAtLevel=1",
        "--enable-native-access=ALL-UNNAMED",
        "-Xms4g",
        "-Xmn2g",
        "-XX:+UseParallelGC"
    )
}

jlink {
    addOptions("--strip-java-debug-attributes", "--compress", "1", "--no-header-files", "--no-man-pages")
    addOptions("--add-modules", "ch.qos.logback.classic,ch.qos.logback.core,java.naming,java.sql")

    addExtraDependencies(
        "picocli",
        "commons-compress",
        "kotlinx-serialization-json",
        "kotlinx-serialization-core",
        "kotlinx-coroutines-core",
    )

    forceMerge(".*")

    launcher {
        name = "openvadl"
    }

    mergedModule {
        excludeProvides(mapOf("service" to "jakarta.servlet.ServletContainerInitializer"))
        requires("java.sql")
    }

    moduleName.set("openvadl")
    mergedModuleName.set("openvadl")
    mainClass.set("vadl.cli.Main")

    if (project.hasProperty("jlinkAllPlatforms")) {
        targetPlatform("linux-x64", "/jdks/jdk-linux-x64")
        targetPlatform("linux-arm64", "/jdks/jdk-linux-arm64")
        targetPlatform("macos-arm64", "/jdks/jdk-macos-arm64")
        targetPlatform("win-x64", "/jdks/jdk-win-x64")
        targetPlatform("win-arm64", "/jdks/jdk-win-arm64")
    }
}

tasks.prepareMergedJarsDir {
    rootProject.allprojects
        .filter { it.tasks.names.contains("jar") }
        .forEach { currentProject ->
            dependsOn(currentProject.tasks.named("jar"))
            inputs.files(currentProject.tasks.named("jar"))
        }

    doLast {
        copy {
            from(zipTree(tasks.jar.get().archiveFile))
            into("${layout.buildDirectory.get()}/jlinkbase/mergedjars")
        }
    }
}
