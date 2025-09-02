plugins {
    application
    id("org.graalvm.buildtools.native") version "0.10.2"
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

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    applicationName = "openvadl"
    mainClass.set("vadl.cli.Main")
}

graalvmNative {
    binaries {
        named("main") {
            // required to include templates
            resources.autodetect()
            imageName.set("openvadl")
            mainClass.set(application.mainClass)
            buildArgs.addAll("-O4", "--gc=epsilon") // Use -0b for faster dev builds, -O4 for production
            // some tools require network access to download source code (QEMU, LLVM)
            buildArgs.add("--enable-url-protocols=https")

        }
    }
}

tasks.startScripts {
    defaultJvmOpts = listOf("-XX:TieredStopAtLevel=1")
}

tasks.test {
    useJUnitPlatform()
}
