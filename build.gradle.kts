import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java")
    id("net.ltgt.errorprone") version "3.1.0"
}

group = "ord.vadl"
version = "1.0-SNAPSHOT"

val errorProneVersion = "2.26.1"

repositories {
    mavenCentral()
}

dependencies {
    errorprone("com.uber.nullaway:nullaway:0.10.25")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    errorprone("com.google.errorprone:error_prone_core:$errorProneVersion")
    compileOnly("com.google.errorprone:error_prone_annotations:$errorProneVersion")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation(libs.junit)
}


tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"

//    options.compilerArgs.plusAssign("--enable-preview")

    if (!name.lowercase().contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "at.ac.tuwien.complang.vadl")
            disable("EqualsGetClass")
        }
    }
}

tasks {
    compileTestJava {
        options.errorprone.isEnabled.set(false)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
