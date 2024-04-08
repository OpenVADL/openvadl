import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path

plugins {
    id("java")
    id("net.ltgt.errorprone") version "3.1.0"
    checkstyle
}

group = "vadl"
version = "1.0-SNAPSHOT"

val checkstyleVersion = "10.15.0"
val errorProneVersion = "2.26.1"

repositories {
    mavenCentral()
}

dependencies {
    errorprone("com.uber.nullaway:nullaway:0.10.25")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    errorprone("com.google.errorprone:error_prone_core:$errorProneVersion")
    compileOnly("com.google.errorprone:error_prone_annotations:$errorProneVersion")

    annotationProcessor(project(":vadl-annotations"))
    implementation(project(":vadl-annotations"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

checkstyle {
    toolVersion = checkstyleVersion
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"

//    options.compilerArgs.plusAssign("--enable-preview")

    if (!name.lowercase().contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "vadl")
            disable("EqualsGetClass")
        }
    }
}


tasks {
    compileTestJava {
        options.errorprone.isEnabled.set(false)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


/**************
 * CHECKSTYLE TASK CONFIGS
 *************/

tasks.register("checkstyleAll") {
    project.extensions.extraProperties.set("forceCheckstyle", "true")
    dependsOn("checkstyleMain", "checkstyleTest")
}

val taskCheckstyleMain = tasks.named<Checkstyle>("checkstyleMain") {
    if (project.hasProperty("forceCheckstyle")) {
        maxWarnings = 0;
    }
}

val taskCheckstyleTest = tasks.named<Checkstyle>("checkstyleTest") {
    if (project.hasProperty("forceCheckstyle")) {
        maxWarnings = 0;
    }
}


tasks.register("generateBundledCheckstyleReports") {
    dependsOn("generateCheckstyleMainMdReport", "generateCheckstyleTestMdReport")
    doLast {
        val mainMD =
            taskCheckstyleMain.get().reports.xml.outputLocation.asFile.get()
                .let { File(it.parent, it.nameWithoutExtension + ".md") }
        val testMD =
            taskCheckstyleTest.get().reports.xml.outputLocation.asFile.get()
                .let { File(it.parent, it.nameWithoutExtension + ".md") }
        println("Bundling ${mainMD.name} and ${testMD.name}...")
        val bundleMD = File(mainMD.parent, "report.md")
        // not very efficient but should be fine for the md sizes
        bundleMD.writeText(mainMD.readText())
        bundleMD.appendText("\n\n")
        bundleMD.appendText(testMD.readText())
        println("Report bundled in $bundleMD")
    }
}


tasks.register<Task>("generateCheckstyleMainMdReport") {
    doFirst {
        val checkstyleTask = tasks.named<Checkstyle>("checkstyleMain").get()
        genCheckstyleMdReport(checkstyleTask.reports.xml)
        println("Generated Markdown report for ${checkstyleTask.name}")
    }
}

tasks.register<Task>("generateCheckstyleTestMdReport") {
    doFirst {
        val checkstyleTask = tasks.named<Checkstyle>("checkstyleTest").get()
        genCheckstyleMdReport(checkstyleTask.reports.xml)
        println("Generated Markdown report for ${checkstyleTask.name}")
    }
}

/// Generates a markdown report for the given xml report
/// in same directory as given report
fun genCheckstyleMdReport(xmlReport: SingleFileReport) {
    val reportFile = xmlReport.outputLocation.asFile.get()

    println("Generating markdown report from ${reportFile.name}...")

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
    mdReport.append("<summary>&nbsp;$statusIcon <b>${reportFile.nameWithoutExtension}</b>: Checkstyle found <b>${errors.length}</b> violations</summary>\n\n")
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
    mdFile.writeText(mdReport.toString())
}
