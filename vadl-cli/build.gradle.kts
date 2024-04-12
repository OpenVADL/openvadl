plugins {
    id("java")
}

group = "vadl.cli"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(":vadl"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}