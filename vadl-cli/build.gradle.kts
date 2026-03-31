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
    implementation(project(":vadl-core"))
    implementation(project(":vadl-pass-api"))
    implementation(project(":vadl-frontend"))
    implementation(project(":vadl-rtl"))
    implementation(project(":vadl-iss"))
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
        named("main") {
            resources.autodetect()
            imageName.set("openvadl")
            mainClass.set(application.mainClass)
            buildArgs.addAll("-O2", "--gc=epsilon")
            buildArgs.add("--enable-url-protocols=https")
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
        }
    }
}

tasks.startScripts {
    defaultJvmOpts = listOf(
        "-XX:TieredStopAtLevel=1",
        "--enable-native-access=ALL-UNNAMED",
    )
}

jlink {
    addOptions("--strip-java-debug-attributes", "--compress", "1", "--no-header-files", "--no-man-pages")
    addOptions("--add-modules", "ch.qos.logback.classic,ch.qos.logback.core,java.naming,java.sql")

    addExtraDependencies("picocli", "commons-compress")

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
