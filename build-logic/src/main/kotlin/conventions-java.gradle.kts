import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import utils.libs

plugins {
    java
    checkstyle
    id("net.ltgt.errorprone")
}


repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.test {
    useJUnitPlatform()
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configDirectory.set(project.projectDir.resolve("../config/checkstyle/"))
    sourceSets = listOf()
    maxWarnings = 0
}

dependencies {
    add("errorprone", libs.nullaway)
    add("errorprone", libs.errorprone.core)
    compileOnly(libs.jsr305)
    compileOnly(libs.errorprone.annotations)
    compileOnly(libs.jetbrains.annotations)

    implementation(libs.guava)
    implementation(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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