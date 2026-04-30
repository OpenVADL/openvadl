import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import util.registerBenchmarkSuiteTask
import util.registerBenchmarkTestTask
import vadl.GenerateCocoParserTask
import java.util.*

plugins {
    id("conventions-jvm")
    id("conventions-benchmark-tests")
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

tasks.processTestResources {
    exclude("**/__pycache__/**", "**/*.pyc")
}

tasks.withType<Test>().configureEach {
    environment("PROJECT_ROOT", rootDir.absolutePath)
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

val testIssBenchmarkRiscv = registerBenchmarkTestTask(
    name = "test-iss-benchmark-riscv",
    description = "Runs the ISS RISC-V benchmark tests",
    includePattern = "vadl.iss.riscv.IssRiscvEmbenchBenchmarkTest"
)

val testIssBenchmarkAarch64 = registerBenchmarkTestTask(
    name = "test-iss-benchmark-aarch64",
    description = "Runs the ISS AArch64 benchmark tests",
    includePattern = "vadl.iss.aarch64.IssAarch64EmbenchBenchmarkTest"
)

registerBenchmarkSuiteTask(
    name = "test-iss-benchmark",
    description = "Runs the ISS benchmark tests",
    dependencies = listOf(testIssBenchmarkRiscv.get(), testIssBenchmarkAarch64.get())
)

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
