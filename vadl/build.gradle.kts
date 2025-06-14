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

import vadl.CocoR_gradle
import java.util.*

plugins {
    id("vadl.CocoR")
}


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
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.apache.commons:commons-text:1.10.0")


    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testImplementation("org.testcontainers:testcontainers:1.20.6")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testAnnotationProcessor(project(":java-annotations"))
    // Helps getting test files small and concise
    testImplementation("org.apache.velocity:velocity-engine-core:2.3")
    testImplementation("net.jqwik:jqwik:1.9.0")
    testImplementation("org.yaml:snakeyaml:2.2")
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

// Register the custom task with your configuration
tasks.register<CocoR_gradle.GenerateCocoParserTask>("generateCocoParser") {
    group = "build"
    inputFiles.from("main/vadl/ast/vadl.ATG")
    parserFrame.set(project.file("main/vadl/ast/Parser.frame"))
    outputDir.set(outputDir.get().dir("vadl/ast"))
    cocoJar.set(project.file("libs/Coco.jar"))
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
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    reports {
        junitXml.required.set(true)
    }
}

// generators to be tested separately
val generators = listOf("iss", "lcb")

for (gen in generators) {
    tasks.register<Test>("test-$gen") {
        group = "verification"
        // fail fast, so we don't try to rebuild all failing images over and over
        failFast = true
        val pkg = "vadl.$gen"
        description = "Runs tests for the $pkg package"
        filter {
            includeTestsMatching("$pkg.*")
        }
    }
}


tasks.register<Test>("test-others") {
    group = "verification"
    val exclPkgs = generators.joinToString(", ") { "vadl.$it" }
    description = "Runs tests for vadl.* packages excluding $exclPkgs"

    filter {
        includeTestsMatching("vadl.*")
        for (gen in generators) {
            val pkg = "vadl.$gen"
            excludeTestsMatching("$pkg.*")
        }
    }
}
