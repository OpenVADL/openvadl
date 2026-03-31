import groovy.util.Node
import org.jetbrains.gradle.ext.settings

plugins {
    id("org.jetbrains.gradle.plugin.idea-ext")
}

fun Node.ensureChild(name: String, attributes: Map<String, String> = emptyMap()): Node {
    val existingChild = children().filterIsInstance<Node>().find { child ->
        child.name() == name && attributes.all { (key, value) -> child.attribute(key) == value }
    }

    return existingChild ?: appendNode(name, attributes)
}

idea.project.settings {
    withIDEAFileXml("workspace.xml") {
        val root = asNode()

        val vcsManagerConfig = root.ensureChild("component", mutableMapOf("name" to "VcsManagerConfiguration"))
        vcsManagerConfig.ensureChild(
            "option",
            mutableMapOf("name" to "OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT"),
        ).also { it.attributes()?.put("value", "true") }

        val updateCopyrightHandler = root.ensureChild(
            "component",
            mutableMapOf("name" to "UpdateCopyrightCheckinHandler"),
        )
        updateCopyrightHandler.ensureChild("option", mutableMapOf("name" to "UPDATE_COPYRIGHT"))
            .also { it.attributes()["value"] = "true" }
    }
}
