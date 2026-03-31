import vadl.GenerateCocoParserTask

plugins {
    id("conventions-jvm")
}

dependencies {
    annotationProcessor(project(":java-annotations"))
    compileOnly(project(":java-annotations"))
    implementation(project(":vadl-core"))
    implementation(libs.guava)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/coco/java/main")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
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
