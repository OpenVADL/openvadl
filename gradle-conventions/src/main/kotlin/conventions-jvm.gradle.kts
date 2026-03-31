import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

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
    add("errorprone", "com.uber.nullaway:nullaway:0.10.25")
    add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
    add("errorprone", "com.google.errorprone:error_prone_core:2.43.0")
    add("compileOnly", "com.google.errorprone:error_prone_annotations:2.43.0")
    add("compileOnly", "org.jetbrains:annotations:24.0.1")
    add("implementation", "ch.qos.logback:logback-classic:1.5.24")
    add("testImplementation", platform("org.junit:junit-bom:5.11.4"))
    add("testImplementation", "org.junit.jupiter:junit-jupiter")
    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
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
