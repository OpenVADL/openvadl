plugins {
}

group = "vadl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    implementation(project(":java-annotations"))
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("commons-io:commons-io:2.16.1")

    testImplementation("org.awaitility:awaitility:4.2.1")
    testImplementation("org.testcontainers:testcontainers:1.20.0")
    testImplementation("org.testcontainers:junit-jupiter:1.20.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testAnnotationProcessor(project(":java-annotations"))
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/coco/java/main")
        }
    }
}

tasks.withType<JavaCompile> {
    dependsOn("generateCocoParser")
}

tasks.withType<Checkstyle> {
    doFirst {
        exclude { f ->
            // NOTE: we cannot exclude all tests here but we could disable the checkstyleTest target.
            val absolute = f.file.absolutePath
            absolute.contains("build/generated/")
        }
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

// Register the custom task with your configuration
tasks.register<GenerateCocoParserTask>("generateCocoParser") {
    inputFiles.from("main/vadl/ast/vadl.ATG")
    outputDir.set(outputDir.get().dir("vadl/ast"))
    cocoJar.set(project.file("libs/Coco.jar"))
}

open class GenerateCocoParserTask : DefaultTask() {
    @InputFiles
    val inputFiles: ConfigurableFileCollection = project.files()

    @InputFile
    val cocoJar = project.objects.fileProperty()

    @OutputDirectory
    val outputDir =
        project.objects.directoryProperty().convention(
            project.layout.buildDirectory.dir("generated/sources/coco/java/main"),
        )

    @TaskAction
    fun generate() {
        // Ensure the output directory exists
        val outputDirFile = outputDir.get().asFile
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs()
        }

        inputFiles.files.forEach {
            println("Generating from $it...")
            project.exec {
                commandLine("java", "-jar", cocoJar.get().asFile.absolutePath, "-o", outputDirFile.path, it)
            }
            println("------")
        }
    }
}
