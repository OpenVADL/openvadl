plugins {
    id("java")
}


group = "vadl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    test {
        resources.setSrcDirs(listOf("resources"))
    }
}

tasks.test {
    useJUnitPlatform()
}
