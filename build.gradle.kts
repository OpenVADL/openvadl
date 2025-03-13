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

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("net.ltgt.gradle:gradle-errorprone-plugin:2.0.1")
    }
}

plugins {
    id("java")
    checkstyle
    id("net.ltgt.errorprone") version "4.0.1" apply false

    // log executed tests
    id("com.adarshr.test-logger") version "4.0.0"

    // custom plugins
    id("vadl.IdeConfigPlugin")
}

allprojects {
    group = "vadl"
    version = "0.1.0-SNAPSHOT"
}


subprojects {
    plugins.apply("java")
    plugins.apply("net.ltgt.errorprone")
    plugins.apply("checkstyle")
    plugins.apply("com.adarshr.test-logger")

    val errorProneVersion by extra("2.26.1")

    extra {
        errorProneVersion
    }

    checkstyle {
        toolVersion = "10.15.0"
        // configFile = project.projectDir.resolve("../config/checkstyle/checkstyle.xml")
        configDirectory.set(project.projectDir.resolve("../config/checkstyle/"))
        sourceSets = listOf()
        maxWarnings = 0
    }

    dependencies {
        add("errorprone", "com.uber.nullaway:nullaway:0.10.25")
        add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
        add("errorprone", "com.google.errorprone:error_prone_core:$errorProneVersion")
        add("compileOnly", "com.google.errorprone:error_prone_annotations:$errorProneVersion")
        add("compileOnly", "org.jetbrains:annotations:24.0.1")
    }


    sourceSets {
        main {
            java {
                srcDir("main")
                exclude("main/resources/**")
            }
            resources {
                srcDir("main/resources")
            }
        }

        test {
            java {
                srcDir("test")
                exclude("test/resources/**")
            }
            resources {
                srcDir("test/resources")
            }
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"

        if (!name.toLowerCase().contains("test")) {
            options.errorprone {
                check("NullAway", CheckSeverity.ERROR)
                option("NullAway:AnnotatedPackages", "vadl,java-annotations")
                disable("EqualsGetClass", "StringCaseLocaleUsage")
                excludedPaths.set(".*/generated/sources/coco/java/main/vadl/ast/*.*")
            }
        }

        if (project.hasProperty("FailOnWarnings")) {
            options.compilerArgs.add("-Werror")
        }
    }

    tasks.withType<JavaExec> {
        workingDir = rootProject.projectDir
        outputs.upToDateWhen { false }
    }

    tasks {
        compileTestJava {
            options.errorprone.isEnabled.set(false)
        }
    }
}

/**************
 * CI TEST TASK CONFIGS
 *************/

tasks.register<Test>("test-common") {
    dependsOn(":vadl:test-others", ":vadl-cli:test", ":java-annotations:test")
}

/**************
 * CHECKSTYLE TASK CONFIGS
 *************/

tasks.register("checkstyleAll") {
    val checkstyleTasks =
        subprojects
            .map { setOf(it.tasks.checkstyleMain, it.tasks.checkstyleTest) }
            .flatten()

    dependsOn(checkstyleTasks)
}

