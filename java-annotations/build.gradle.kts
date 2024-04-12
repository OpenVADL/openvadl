plugins {
    id("java")
}

group = "vadl.javaanotations"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
}
