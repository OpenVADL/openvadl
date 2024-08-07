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
    implementation("org.junit.jupiter:junit-jupiter-params")
    implementation("org.hamcrest:hamcrest:2.2")
    implementation("org.junit.platform:junit-platform-launcher:1.10.2")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")

    // Helps getting test files small and concise
    implementation("org.apache.velocity:velocity-engine-core:2.3")
}

tasks.withType<JavaCompile> {
    options.errorprone.isEnabled.set(false)
}

tasks.test {
    useJUnitPlatform()
}