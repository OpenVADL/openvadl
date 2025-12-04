plugins {
    id("java")
}

group = "vadl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    test {
        resources.setSrcDirs(listOf("resources"))
    }
}

tasks.test {
    useJUnitPlatform()
}