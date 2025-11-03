plugins {
    application
    id("io.github.rascmatt.z3") version "1.0.2"
    id("org.graalvm.buildtools.native") version "0.11.2"
}

group = "vadl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":vadl"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("org.apache.commons:commons-compress:1.27.1")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
