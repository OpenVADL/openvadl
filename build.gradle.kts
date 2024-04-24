import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

buildscript {
    repositories {
        mavenCentral()  // or google() if it's there
        gradlePluginPortal()
    }
    dependencies {
        classpath("net.ltgt.gradle:gradle-errorprone-plugin:2.0.1")  // Check for the latest version
    }
}

plugins {
    id("java")
    id("net.ltgt.errorprone") version "2.0.1" apply false
}

subprojects {
    plugins.apply("java")

    val checkstyleVersion = "10.15.0"
    val errorProneVersion = "2.26.1"


    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        add("errorprone", "com.uber.nullaway:nullaway:0.10.25")
        add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
        add("errorprone", "com.google.errorprone:error_prone_core:$errorProneVersion")
        add("compileOnly", "com.google.errorprone:error_prone_annotations:$errorProneVersion")
    }


    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"

        if (!name.toLowerCase().contains("test")) {
            options.errorprone {
                check("NullAway", CheckSeverity.ERROR)
                option("NullAway:AnnotatedPackages", "vadl,java-annotations")
                disable("EqualsGetClass")
            }
        }
    }

    tasks {
        compileTestJava {
            options.errorprone.isEnabled.set(false)
        }
    }

}

