package utils

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

open class GenerateCocoParserTask @Inject constructor(
    private val execOps: ExecOperations
) : DefaultTask() {
    @InputFiles
    val inputFiles: ConfigurableFileCollection = project.files()

    @InputFile
    val cocoJar = project.objects.fileProperty()

    @InputFile
    @Optional
    val parserFrame = project.objects.fileProperty()

    @OutputDirectory
    val outputDir =
        project.objects.directoryProperty().convention(
            project.layout.buildDirectory.dir("generated/sources/coco/java/main"),
        )

    @TaskAction
    fun generate() {
        // Ensure the output directory exists
        val outputDirFile = outputDir.get().asFile
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs()
        }

        val opts = mutableListOf<String>()
        if (parserFrame.isPresent) {
            opts.add("-P")
            opts.add(parserFrame.get().asFile.absolutePath)
        }

        inputFiles.files.forEach {
            println("Generating from $it...")
            execOps.exec {
                commandLine(
                    "java", "-jar", cocoJar.get().asFile.absolutePath,
                    "-o", outputDirFile.path,
                    *opts.toTypedArray(),
                    it
                )
            }
            println("------")
        }
    }
}
