import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.io.FileWriter
import javax.xml.parsers.DocumentBuilderFactory

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("net.ltgt.gradle:gradle-errorprone-plugin:2.0.1")
    }
}

plugins {
    id("java")
    checkstyle
    id("net.ltgt.errorprone") version "2.0.1" apply false
}


subprojects {
    plugins.apply("java")
    plugins.apply("net.ltgt.errorprone")
    plugins.apply("checkstyle")

    val errorProneVersion by extra("2.26.1")

    extra {
        errorProneVersion
    }

    checkstyle {
        toolVersion = "10.15.0"
        configFile = project.projectDir.resolve("../config/checkstyle/checkstyle.xml")
        sourceSets = listOf()
        maxWarnings = 0
    }

    dependencies {
        add("errorprone", "com.uber.nullaway:nullaway:0.10.25")
        add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
        add("errorprone", "com.google.errorprone:error_prone_core:$errorProneVersion")
        add("compileOnly", "com.google.errorprone:error_prone_annotations:$errorProneVersion")
        add("compileOnly", "org.jetbrains:annotations:24.0.1")
    }


    sourceSets {
        main {
            java {
                srcDir("main")
                exclude("main/resources/**")
            }
            resources {
                srcDir("main/resources")
            }
        }

        test {
            java {
                srcDir("test")
                exclude("test/resources/**")
            }
            resources {
                srcDir("test/resources")
            }
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"

        if (!name.toLowerCase().contains("test")) {
            options.errorprone {
                check("NullAway", CheckSeverity.ERROR)
                option("NullAway:AnnotatedPackages", "vadl,java-annotations")
                disable("EqualsGetClass")
            }
        }
    }

    tasks {
        compileTestJava {
            options.errorprone.isEnabled.set(false)
        }
    }

}


/**************
 * CHECKSTYLE TASK CONFIGS
 *************/

tasks.register("checkstyleAll") {
    val checkstyleTasks = subprojects
        .map { setOf(it.tasks.checkstyleMain, it.tasks.checkstyleTest) }
        .flatten()

    dependsOn(checkstyleTasks)

    // All checkstyle tasks must be finalized by
    // the generateCheckstyleReport task
    subprojects.forEach {
        it.tasks.withType<Checkstyle> {
            finalizedBy(generateCheckstyleReport)
        }
    }
}


val generateCheckstyleReport = tasks.register("generateCheckstyleReport") {
    doLast {

        val projectCheckstylePairs = mutableListOf<Pair<Project, Checkstyle>>()
        val failures = mutableListOf<String>()
        subprojects.forEach { subproject ->
            subproject.tasks.withType<JavaCompile> {
                @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY", "UNNECESSARY_SAFE_CALL")
                state.failure?.localizedMessage?.let {
                    failures.add(it)
                }
            }
            subproject.tasks.withType<Checkstyle>().forEach { task ->
                projectCheckstylePairs.add(Pair(subproject, task))
            }
        }

        val projectReportDir = tasks.checkstyleMain.get().reports.xml.outputLocation.asFile.get().parentFile
        val bundledReportFile = File(projectReportDir, "report.md")

        // create report dir if necessary
        if (!projectReportDir.exists()) {
            projectReportDir.mkdirs()
        }

        if (failures.isNotEmpty()) {
            val errorBlocks = failures.joinToString("\n") { f ->
                """
                ```
                $f
                ```
                """.trimIndent()
            }
            bundledReportFile.writeText("### ❌ No Checkstyle Report\n$errorBlocks")
            return@doLast
        }

        logger.info("Generating Checkstyle markdown reports...")
        FileWriter(bundledReportFile).use { writer ->
            writer.append("### Checkstyle Report\n")
            // same location as
            projectCheckstylePairs.forEach { (project, task) ->
                val checkName = "${project.name} (${task.name.removePrefix("checkstyle")})"
                val subReport = genCheckstyleMdReport(checkName, task.reports.xml) ?: return@forEach

                // not very efficient but should be fine for the md sizes
                writer.append(subReport)
                writer.append("\n\n")
            }

        }

        logger.info("✅ Bundled report written to ${bundledReportFile.absolutePath}.")

    }

}

///// Generates a markdown report for the given xml report
///// in same directory as given report
fun genCheckstyleMdReport(checkName: String, xmlReport: SingleFileReport): String? {
    val reportFile = xmlReport.outputLocation.asFile.get()

    if (!reportFile.exists()) {
        logger.info("${reportFile.path}: XML Report does not exist... skip.")
        return null
    }

    logger.info("${reportFile.path}: Generating markdown report for $checkName... ")

    // Initialize XML Document Builder
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val doc = dBuilder.parse(reportFile)
    doc.documentElement.normalize()

    val errors = doc.getElementsByTagName("error")
    val statusIcon = if (errors.length == 0) "✅" else "❌"

    // Start Markdown report
    val mdReport = StringBuilder()
    mdReport.append("<details>\n")
    mdReport.append("<summary>&nbsp;$statusIcon <b>${checkName}</b>: Checkstyle found <b>${errors.length}</b> violations</summary>\n\n")
    mdReport.append("```\n")


    // Iterate through XML nodes and append to Markdown
    val fileList = doc.getElementsByTagName("file")

    for (i in 0 until fileList.length) {
        val fileNode = fileList.item(i)
        if (fileNode.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
            val elem = fileNode as org.w3c.dom.Element
            val fileName = elem.getAttribute("name")

            val errorList = elem.getElementsByTagName("error")
            if (errorList.length != 0)
                mdReport.append("File: $fileName\n")
            for (j in 0 until errorList.length) {
                val errorNode = errorList.item(j)
                if (errorNode.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val errorElem = errorNode as org.w3c.dom.Element
                    val line = errorElem.getAttribute("line")
                    val severity = errorElem.getAttribute("severity")
                    val message = errorElem.getAttribute("message")
                    mdReport.append("    Line $line: $severity:\t$message\n")
                }
            }
        }
    }

    mdReport.append("```\n")
    mdReport.append("</details>")

    // save markdown report
    val mdFile = File(reportFile.parent, reportFile.nameWithoutExtension + ".md")
    val finalString = mdReport.toString()
    mdFile.writeText(finalString)
    return finalString
}
