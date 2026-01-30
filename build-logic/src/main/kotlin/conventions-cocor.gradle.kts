
import utils.GenerateCocoParserTask

plugins {
    id("conventions-java")
}

sourceSets {
    main {
        java {
            srcDir("build/generated/sources/coco/java/main")
        }
    }
}

// Register the custom task with your configuration

val generateCocoParser = tasks.register<GenerateCocoParserTask>("generateCocoParser") {
    group = "build"

    inputFiles.from("main/vadl/ast/vadl.ATG")
    parserFrame.set(layout.projectDirectory.file("main/vadl/ast/Parser.frame"))
    cocoJar.set(layout.projectDirectory.file("libs/Coco.jar"))

    outputDir.set(
        layout.buildDirectory.dir("generated/sources/coco/java/main/vadl/ast")
    )
}
