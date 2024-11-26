import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java")
}

group = "vadl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(":vadl"))

    implementation(platform("org.junit:junit-bom:5.10.2"))
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.assertj:assertj-core:3.26.0")
    implementation("org.hamcrest:hamcrest:2.2")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("net.jqwik:jqwik:1.9.0")

    implementation("org.awaitility:awaitility:4.2.1")
    implementation("org.testcontainers:testcontainers:1.20.0")
    implementation("org.testcontainers:junit-jupiter:1.20.0")

    implementation("org.apache.commons:commons-compress:1.21")

    // Helps getting test files small and concise
    implementation("org.apache.velocity:velocity-engine-core:2.3")
}

tasks.withType<JavaCompile> {
    options.errorprone.isEnabled.set(false)
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("skipped", "failed")
    }
}

