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
    implementation("org.hamcrest:hamcrest:2.2")
    implementation("org.yaml:snakeyaml:2.2")

    implementation("org.awaitility:awaitility:4.2.1")
    implementation("org.testcontainers:testcontainers:1.20.0")
    implementation("org.testcontainers:junit-jupiter:1.20.0")

    // Helps getting test files small and concise
    implementation("org.apache.velocity:velocity-engine-core:2.3")
}

tasks.withType<JavaCompile> {
    options.errorprone.isEnabled.set(false)
}

tasks.test {
    useJUnitPlatform()
}