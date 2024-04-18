plugins {
}

group = "vadl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    implementation(project(":java-annotations"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testAnnotationProcessor(project(":java-annotations"))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}
