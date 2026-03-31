import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import util.libs

plugins {
    java
    checkstyle
    id("net.ltgt.errorprone")
    id("com.adarshr.test-logger")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

checkstyle {
    toolVersion = "10.15.0"
    configDirectory.set(rootDir.resolve("config/checkstyle"))
    sourceSets = listOf()
    maxWarnings = 0
}

dependencies {
    errorprone(libs.nullaway)
    compileOnly(libs.jsr305)
    errorprone(libs.errorprone.core)
    compileOnly(libs.errorprone.annotations)
    compileOnly(libs.jetbrains.annotations)
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

tasks.withType<JavaCompile>().configureEach {
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

tasks.withType<JavaExec>().configureEach {
    standardInput = System.`in`
    workingDir = rootProject.projectDir
    outputs.upToDateWhen { false }
}

tasks.named<JavaCompile>("compileTestJava").configure {
    options.errorprone.isEnabled.set(false)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
