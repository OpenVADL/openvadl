import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import vadl.GenerateCocoParserTask

plugins {
    idea
    id("conventions-jvm")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.z3)
}

dependencies {
    implementation(project(":vadl"))

    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))

    implementation(libs.thymeleaf)
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)
    implementation(libs.z3.bootstrap)
    implementation(kotlin("stdlib-jdk8"))

    testCompileOnly(project(":java-annotations"))
    testCompileOnly(libs.jsr305)
    testAnnotationProcessor(project(":java-annotations"))
    testImplementation(libs.buildkitcli)
    testImplementation(libs.assertj.core)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.velocity.engine.core)
    testImplementation(libs.jqwik)
    testImplementation(libs.snakeyaml)
}

kotlin {
    jvmToolchain(25)
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/coco/java/main")
        }
    }
    test {
        resources {
            srcDir(project(":vadl-test").layout.projectDirectory.dir("resources"))
        }
    }
}

idea {
    module {
        generatedSourceDirs.add(file("build/generated/sources/coco/java/main"))
    }
}

tasks.matching { it is KotlinCompile || it is JavaCompile }.configureEach {
    dependsOn("generateCocoParser")
}

tasks.withType<Checkstyle>().configureEach {
    doFirst {
        exclude { fileTreeElement ->
            fileTreeElement.file.absolutePath.contains("build/generated/")
        }
    }
}

tasks.register<GenerateCocoParserTask>("generateCocoParser") {
    group = "build"
    inputFiles.from("main/vadl/ast/vadl.ATG")
    parserFrame.set(project.file("main/vadl/ast/Parser.frame"))
    outputDir.set(outputDir.get().dir("vadl/ast"))
    cocoJar.set(rootProject.file("vadl/libs/Coco.jar"))
}

tasks.withType<Test>().configureEach {
    environment("PROJECT_ROOT", rootDir.absolutePath)
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    maxHeapSize = "2g"
    reports {
        junitXml.required.set(true)
    }
}

tasks.processTestResources {
    exclude("**/__pycache__/**", "**/*.pyc")
}
