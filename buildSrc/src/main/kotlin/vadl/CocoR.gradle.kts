// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl


open class GenerateCocoParserTask : DefaultTask() {
    @InputFiles
    val inputFiles: ConfigurableFileCollection = project.files()

    @InputFile
    val cocoJar = project.objects.fileProperty()

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

        inputFiles.files.forEach {
            println("Generating from $it...")
            project.exec {
                commandLine("java", "-jar", cocoJar.get().asFile.absolutePath, "-o", outputDirFile.path, it)
            }
            println("------")
        }
    }
}
