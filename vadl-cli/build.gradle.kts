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
            // required to include templates
            resources.autodetect()
            imageName.set("openvadl")
            mainClass.set(application.mainClass)
            buildArgs.addAll("-O4", "--gc=epsilon") // Use -0b for faster dev builds, -O4 for production
        }
    }

    agent {
        enabled.set(true)

        // TODO: Add this again, after we used our own reflect package
        // callerFilterFiles.from("${projectDir}/user-code-filter.json")
        // accessFilterFiles.from("${projectDir}/user-code-filter.json")

        metadataCopy {
            inputTaskNames.add("run")
            outputDirectories.add("main/resources/META-INF/native-image/vadl")
            mergeWithExisting.set(true)
        }
    }
}

task<Exec>("collectNativeMetadata") {
    workingDir = rootDir
    commandLine("bash", "scripts/collect-native-metadata.sh")
}

tasks.startScripts {
    defaultJvmOpts = listOf("-XX:TieredStopAtLevel=1")
}

tasks.test {
    useJUnitPlatform()
}
