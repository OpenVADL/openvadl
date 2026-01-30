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

import groovy.util.Node
import org.jetbrains.gradle.ext.settings

plugins {
    id("org.jetbrains.gradle.plugin.idea-ext")
}

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
