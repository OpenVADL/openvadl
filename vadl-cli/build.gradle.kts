plugins {
    application
    id("io.github.rascmatt.z3") version "1.0.2"
    id("org.graalvm.buildtools.native") version "0.11.2"
    id("org.beryx.jlink") version "3.2.0"
}


group = "vadl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":vadl"))
    implementation(project(":vadl-lsp"))
    implementation(libs.picocli)
    implementation(libs.commons.compress)
    annotationProcessor(libs.picocli.codegen)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    applicationName = "openvadl"
    mainClass.set("vadl.cli.Main")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

graalvmNative {
    binaries {
        named("main") {
            // required to include templates
            resources.autodetect()
            imageName.set("openvadl")
            mainClass.set(application.mainClass)
            // we use -O2 as currently compiling with -O3 doesn't terminate.
            buildArgs.addAll("-O2", "--gc=epsilon")
            // some tools require network access to download source code (QEMU, LLVM)
            buildArgs.add("--enable-url-protocols=https")
            // Z3 platform binaries are loaded via JNI
            buildArgs.add("--enable-native-access=ALL-UNNAMED")

        }
    }
}

tasks.startScripts {
    defaultJvmOpts = listOf(
        "-XX:TieredStopAtLevel=1",
        // Z3 platform binaries are loaded via JNI
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
}

jlink {
    // We use --strip-java-debug-attributes instead of --strip-debug, as --strip-debug also
    // applies native debug symbol stripping, which is not possible for cross-builds.
    // JDK-8219257 and JDK-8219207
    // Further, we are using --compress 1 instead of 2, as the distributed package is already zip, while the
    // zip compress flag (2) adds runtime overhead.
    addOptions("--strip-java-debug-attributes", "--compress", "1", "--no-header-files", "--no-man-pages")
    // Add logback modules for SLF4J logging, java.naming required by logback, and java.sql required by Thymeleaf
    addOptions("--add-modules", "ch.qos.logback.classic,ch.qos.logback.core,java.naming,java.sql")

    // Ensure picocli and commons-compress modular JARs are available during module-info compilation
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

    // Target platforms to build the language server for.
    // If the gradle property `-PjlinkAllPlatforms` is passed, we run jlink for all
    // target platforms. In this case we assume that this is executed
    // within an ghcr.io/openvadl/java-runtime-builder docker container.
    // Otherwise, only the host platform is build.
    if (project.hasProperty("jlinkAllPlatforms")) {
        targetPlatform("linux-x64", "/jdks/jdk-linux-x64")
        targetPlatform("linux-arm64", "/jdks/jdk-linux-arm64")
        targetPlatform("macos-arm64", "/jdks/jdk-macos-arm64")
        targetPlatform("win-x64", "/jdks/jdk-win-x64")
        targetPlatform("win-arm64", "/jdks/jdk-win-arm64")
    }
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
