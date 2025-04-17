import org.asciidoctor.gradle.jvm.pdf.AsciidoctorPdfTask

plugins {
    id("org.antora") version "1.0.0"
    id("org.asciidoctor.jvm.pdf") version "4.0.4"
    id("org.asciidoctor.jvm.gems") version "4.0.4"
}

apply(plugin = "java")

version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    ruby.gems()
}

val refmanPdf = tasks.register<AsciidoctorPdfTask>("refman-pdf") {
    group = "documentation"

    baseDirFollowsSourceDir()
    sourceDir(project.projectDir)
    sources("refman-pdf.adoc")
    outputDirProperty.set(file("${layout.buildDirectory.get()}/docs/pdf"))

    // Set attributes for the theme
    attributes(
        mapOf(
            "pdf-theme" to "refman-theme.yml",
            "pdf-themesdir" to projectDir
        )
    )

}