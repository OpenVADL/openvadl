import vadl.GenerateCocoParserTask

plugins {
    idea
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl"))

    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))

    implementation(libs.guava)

    testCompileOnly(project(":java-annotations"))
    testCompileOnly(libs.jsr305)
    testAnnotationProcessor(project(":java-annotations"))
    testImplementation(libs.assertj.core)
    testImplementation(libs.archunit.junit5)
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

tasks.matching { it is JavaCompile }.configureEach {
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
