import java.util.*

plugins {
    id("conventions-jvm")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.z3)
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))
    implementation(project(":vadl-core"))
    implementation(project(":vadl-pass-api"))
    implementation(project(":vadl-frontend"))
    implementation(project(":vadl-vdt"))
    implementation(libs.thymeleaf)
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)

    implementation(libs.z3.bootstrap)
    implementation(kotlin("stdlib-jdk8"))

    testCompileOnly(project(":java-annotations"))
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
    test {
        resources {
            srcDir(project(":vadl-test").layout.projectDirectory.dir("resources"))
        }
    }
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
