plugins {
    id("conventions-jvm")
    alias(libs.plugins.z3)
}

group = "vadl"
version = "unspecified"

dependencies {
    testCompileOnly(project(":java-annotations"))
    testAnnotationProcessor(project(":java-annotations"))

    testImplementation(project(":vadl-core"))
    testImplementation(project(":vadl-pass-api"))
    testImplementation(project(":vadl-frontend"))
    testImplementation(project(":vadl-vdt"))
    testImplementation(project(":vadl-rtl"))
    testImplementation(project(":vadl-iss"))
    testImplementation(project(":vadl-lcb-gcb"))

    testImplementation(libs.guava)
    testImplementation(libs.commons.io)
    testImplementation(libs.commons.lang3)
    testImplementation(libs.commons.text)
    testImplementation(libs.z3.bootstrap)

    testImplementation(libs.buildkitcli)
    testImplementation(libs.assertj.core)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.velocity.engine.core)
    testImplementation(libs.jqwik)
    testImplementation(libs.snakeyaml)
}

sourceSets {
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("resources"))
    }
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
