import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import util.registerBenchmarkTestTask
import vadl.GenerateCocoParserTask
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    idea
    id("conventions-jvm")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.z3)
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))
    implementation(libs.thymeleaf)
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)

    implementation(libs.z3.bootstrap)
    implementation(kotlin("stdlib-jdk8"))

    testCompileOnly(project(":java-annotations"))
    testCompileOnly(libs.jsr305)
    testAnnotationProcessor(project(":java-annotations"))
    testImplementation(libs.buildkitcli)
    testImplementation(libs.assertj.core)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.velocity.engine.core)
    testImplementation(libs.jqwik)
    testImplementation(libs.snakeyaml)
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

idea {
    module {
        generatedSourceDirs.add(file("build/generated/sources/coco/java/main"))
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

    val gitVersion = rootProject.version.toString()
    val gitCommit = rootProject.findProperty("git.commit")?.toString()?.take(9) ?: "unknown"
    val gitCommitDate = rootProject.findProperty("git.commit.timestamp")?.toString()?.toLongOrNull()
        ?.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } ?: "unknown"

    outputs.file(versionFile)
    inputs.property("version", gitVersion)
    inputs.property("commit", gitCommit)
    inputs.property("commit.date", gitCommitDate)
    doLast {
        val properties = Properties()
        properties["version"] = gitVersion
        properties["commit"] = gitCommit
        properties["commit.date"] = gitCommitDate

        versionFile.get().asFile.apply {
            parentFile.mkdirs()
            outputStream().use { properties.store(it, null) }
        }
    }
}

tasks.processResources {
    from(createProperties)
}

tasks.processTestResources {
    exclude("**/__pycache__/**", "**/*.pyc")
}

tasks.withType<Test>().configureEach {
    environment("PROJECT_ROOT", rootDir.absolutePath)
    useJUnitPlatform {
        val include = System.getProperty("tags.include")
        val exclude = System.getProperty("tags.exclude")
        val benchmarkTask = name.startsWith("benchmark-iss")

        if (include != null) {
            includeTags(include)
        } else if (!benchmarkTask) {
            excludeTags("BenchmarkTest")
        }

        if (exclude != null) {
            excludeTags(exclude)
        }
    }
    jvmArgs("--enable-preview")
    // Gradles worker default to 512MB which causes OOM errors on larger specs.
    maxHeapSize = "2g"
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

val benchmarkIssRiscv64 = registerBenchmarkTestTask(
    name = "benchmark-iss-riscv64",
    description = "Runs the ISS RISC-V 64 benchmark tests",
    includePattern = "vadl.iss.riscv.IssRiscvEmbenchBenchmarkTest"
)

val benchmarkIssAarch64 = registerBenchmarkTestTask(
    name = "benchmark-iss-aarch64",
    description = "Runs the ISS AArch64 benchmark tests",
    includePattern = "vadl.iss.aarch64.IssAarch64EmbenchBenchmarkTest"
)

val benchmarkIssVectorBench64 = registerBenchmarkTestTask(
    name = "benchmark-iss-vectorbench64",
    description = "Runs the synthetic vector benchmark ISA tests",
    includePattern = "vadl.iss.vectorbench.IssVectorBenchBenchmarkTest"
)

benchmarkIssVectorBench64.configure {
    listOf(
        "VECTORBENCH64_QEMU_BIN",
        "VECTORBENCH64_FILTER",
        "VECTORBENCH64_ITERATION_SCALE",
        "VECTORBENCH64_WARMUP_RUNS",
        "VECTORBENCH64_MEASURED_RUNS",
    ).forEach { prop ->
        val value = project.findProperty(prop)?.toString()
        if (!value.isNullOrBlank()) {
            environment(prop, value)
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
