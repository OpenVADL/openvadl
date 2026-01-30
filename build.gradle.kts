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

plugins {
    id("java")
    checkstyle
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.test.logger)
    // custom plugins
    id("vadl.IdeConfigPlugin")
}


group = "openvadl"
version = "0.0.0-SNAPSHOT"
gitVersioning.apply {

    refs {
        branch(".+") {
            version = "\${ref}-SNAPSHOT"
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    rev {
        version = "\${commit}"
    }
}


subprojects {
    plugins.apply("java")
    libs.plugins.errorprone
    plugins.apply("net.ltgt.errorprone")
    plugins.apply("checkstyle")
    plugins.apply("com.adarshr.test-logger")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    checkstyle {
        toolVersion = libs.versions.checkstyle.get()
        configDirectory.set(project.projectDir.resolve("../config/checkstyle/"))
        sourceSets = listOf()
        maxWarnings = 0
    }

    dependencies {
        add("errorprone", libs.nullaway)
        add("compileOnly", libs.jsr305)
        add("errorprone", libs.errorprone.core)
        add("compileOnly", libs.errorprone.annotations)
        add("compileOnly", libs.jetbrains.annotations)
        add("implementation", libs.logback.classic)
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
        if (!name.lowercase().contains("test")) {
            options.errorprone {
                check("NullAway", CheckSeverity.ERROR)
                option("NullAway:AnnotatedPackages", "vadl,java-annotations")
                disable("EqualsGetClass", "StringCaseLocaleUsage", "EffectivelyPrivate", "ClassInitializationDeadlock")
                excludedPaths.set(".*/generated/sources/.*/java/main/vadl/.*")
            }
        }

        if (project.hasProperty("FailOnWarnings")) {
            options.compilerArgs.add("-Werror")
        }
    }

    tasks.withType<JavaExec> {
        standardInput = System.`in`
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
    val checkstyleTasks = subprojects.map { setOf(it.tasks.checkstyleMain, it.tasks.checkstyleTest) }.flatten()

    dependsOn(checkstyleTasks)
}
