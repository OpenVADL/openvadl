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

plugins {
    alias(libs.plugins.conventions.cocor) apply false
    alias(libs.plugins.conventions.java) apply false
    alias(libs.plugins.conventions.root)
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.test.logger)
}


group = "openvadl"
version = "0.0.0-SNAPSHOT"


/**************
 * CI TEST TASK CONFIGS
 *************/

tasks.register<Test>("test-common") {
    dependsOn(":vadl:test-others", ":vadl-cli:test", ":java-annotations:test")
}

/**************
 * GIT BASE OPENVADL VERSIONING
 *************/

gitVersioning.apply {

    refs {
        branch(".+") {
            version = "\${ref}-SNAPSHOT"
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    rev {
        version = "\${commit}"
    }
}