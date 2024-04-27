plugins {
    id("java")
}

group = "vadl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    errorprone("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.errorprone:error_prone_check_api:2.26.1")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

