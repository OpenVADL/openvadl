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
            imageName.set("openvadl")
            mainClass.set(application.mainClass)
            buildArgs.addAll("-Ob", "--gc=G1") // Use -0b for faster dev builds, -O4 for production
        }
    }
}

tasks.startScripts {
    defaultJvmOpts = listOf("-XX:TieredStopAtLevel=1")
}

tasks.test {
    useJUnitPlatform()
}