// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    alias(libs.plugins.conventions.java)
    alias(libs.plugins.conventions.cocor)
    id("io.github.rascmatt.z3") version "1.0.2"
    kotlin("jvm") version "2.3.0"
}


dependencies {
    api(projects.vadlCommon)
    annotationProcessor(projects.javaAnnotations)
    compileOnly(projects.javaAnnotations)
    implementation(libs.thymeleaf)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)
    implementation(libs.z3.bootstrap)
    implementation(kotlin("stdlib-jdk8"))

    testCompileOnly(projects.javaAnnotations)
    testAnnotationProcessor(projects.javaAnnotations)
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers)
    testImplementation(libs.archunit)
    // Helps getting test files small and concise
    testImplementation(libs.velocity)
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

tasks.matching { it is KotlinCompile || it is JavaCompile }.configureEach {
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


// add the generated open-vadl.properties file to the JAR package.
tasks.processResources {
    from(createProperties)
}

// generates an open-vadl properties file at build time.
// this includes the version of open-vadl
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


tasks.withType<Test> {
    environment("PROJECT_ROOT", rootDir.absolutePath)
    useJUnitPlatform {
        val include = System.getProperty("tags.include")
        val exclude = System.getProperty("tags.exclude")

        if (include != null) {
            includeTags(include)
        } else {
            // Default: exclude fast
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

// generators to be tested separately
val generators = listOf("iss", "lcb", "rtl")

for (gen in generators) {
    tasks.register<Test>("test-$gen") {
        group = "verification"
        // fail fast, so we don't try to rebuild all failing images over and over
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
    val exclPkgs = generators.joinToString(", ") { "vadl.$it" }
    description = "Runs tests for vadl.* packages excluding $exclPkgs"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    filter {
        includeTestsMatching("vadl.*")
        for (gen in generators) {
            val pkg = "vadl.$gen"
            excludeTestsMatching("$pkg.*")
        }
    }
}
