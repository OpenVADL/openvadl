import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import vadl.GenerateCocoParserTask
import java.util.*

plugins {
    id("conventions-jvm")
    kotlin("jvm")
    id("io.github.rascmatt.z3")
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("commons-io:commons-io:2.16.1")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.apache.commons:commons-text:1.10.0")

    implementation("io.github.rascmatt:z3-bootstrap:1.0.0")
    implementation(kotlin("stdlib-jdk8"))

    testCompileOnly(project(":java-annotations"))
    testAnnotationProcessor(project(":java-annotations"))
    testImplementation("io.github.kper:buildkitcli:0.14.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("org.apache.velocity:velocity-engine-core:2.3")
    testImplementation("net.jqwik:jqwik:1.9.0")
    testImplementation("org.yaml:snakeyaml:2.2")
}

kotlin {
    jvmToolchain(25)
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/coco/java/main")
        }
    }
    test {
        resources {
            srcDir(project(":vadl-test").layout.projectDirectory.dir("resources"))
        }
    }
}

tasks.matching { it is KotlinCompile || it is JavaCompile }.configureEach {
    dependsOn("generateCocoParser")
}

tasks.withType<Checkstyle>().configureEach {
    doFirst {
        exclude { fileTreeElement ->
            fileTreeElement.file.absolutePath.contains("build/generated/")
        }
    }
}

tasks.register<GenerateCocoParserTask>("generateCocoParser") {
    group = "build"
    inputFiles.from("main/vadl/ast/vadl.ATG")
    parserFrame.set(project.file("main/vadl/ast/Parser.frame"))
    outputDir.set(outputDir.get().dir("vadl/ast"))
    cocoJar.set(project.file("libs/Coco.jar"))
}

val createProperties by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources")
    val versionFile = outputDir.map { it.file("open-vadl.properties") }

    outputs.file(versionFile)
    doLast {
        val properties = Properties()
        properties["version"] = rootProject.version.toString()
        versionFile.get().asFile.apply {
            parentFile.mkdirs()
            outputStream().use { properties.store(it, null) }
        }
    }
}

tasks.processResources {
    from(createProperties)
}

tasks.withType<Test>().configureEach {
    environment("PROJECT_ROOT", rootDir.absolutePath)
    useJUnitPlatform {
        val include = System.getProperty("tags.include")
        val exclude = System.getProperty("tags.exclude")

        if (include != null) {
            includeTags(include)
        } else {
            excludeTags("BenchmarkTest")
        }

        if (exclude != null) {
            excludeTags(exclude)
        }
    }
    jvmArgs("--enable-preview")
    reports {
        junitXml.required.set(true)
    }
}

val generators = listOf("iss", "lcb", "rtl")

for (gen in generators) {
    tasks.register<Test>("test-$gen") {
        group = "verification"
        failFast = true
        val pkg = "vadl.$gen"
        description = "Runs tests for the $pkg package"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        filter {
            includeTestsMatching("$pkg.*")
        }
    }
}

tasks.register<Test>("test-others") {
    group = "verification"
    val excludedPackages = generators.joinToString(", ") { "vadl.$it" }
    description = "Runs tests for vadl.* packages excluding $excludedPackages"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("vadl.*")
        for (gen in generators) {
            excludeTestsMatching("vadl.$gen.*")
        }
    }
}
