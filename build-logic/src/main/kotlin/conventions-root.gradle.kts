import gradle.kotlin.dsl.accessors._93daea375daf749b91a493da031ba15d.checkstyleMain
import gradle.kotlin.dsl.accessors._93daea375daf749b91a493da031ba15d.checkstyleTest
import groovy.util.Node
import org.jetbrains.gradle.ext.settings

plugins {
    id("org.jetbrains.gradle.plugin.idea-ext")
}

/**************
 * CHECKSTYLE TASK CONFIGS
 *************/

tasks.register("checkstyleAll") {
    val checkstyleTasks = subprojects.map { setOf(it.tasks.checkstyleMain, it.tasks.checkstyleTest) }.flatten()

    dependsOn(checkstyleTasks)
}

/**************
 * INTELLIJ CONFIG MODIFICATION
 *************/

fun Node.ensureChild(name: String, attributes: Map<String, String> = emptyMap()): Node {
    // Search for an existing child node with the specified name and attributes
    val existingChild = children().filterIsInstance<Node>().find { child ->
        child.name() == name && attributes.all { (key, value) -> child.attribute(key) == value }
    }
    // If found, return it; otherwise, create a new child node with the specified attributes
    return existingChild ?: appendNode(name, attributes)
}

// this idea project config enables
// - optimization of imports before commit
// - update of copy right before commit
idea.project.settings {
    // Set <option name="OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT" value="true" />
    withIDEAFileXml("workspace.xml") {
        val root = asNode()

        // Ensure <component name="VcsManagerConfiguration">
        val vcsManagerConfig = root.ensureChild("component", mutableMapOf("name" to "VcsManagerConfiguration"))
        // Ensure <option name="OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT" value="true" /> inside VcsManagerConfiguration
        vcsManagerConfig.ensureChild(
            "option",
            mutableMapOf("name" to "OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT")
        ).also { it.attributes()?.put("value", "true") }

        // Ensure <component name="UpdateCopyrightCheckinHandler">
        val updateCopyrightHandler = root.ensureChild(
            "component",
            mutableMapOf("name" to "UpdateCopyrightCheckinHandler")
        )
        // Ensure <option name="UPDATE_COPYRIGHT" value="true" /> inside UpdateCopyrightCheckinHandler
        updateCopyrightHandler.ensureChild("option", mutableMapOf("name" to "UPDATE_COPYRIGHT"))
            .also { it.attributes()["value"] = "true" }
    }
}
