plugins {
    id("conventions-idea")
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.kotlin.jvm) apply false
}

group = "openvadl"
version = "0.0.0-SNAPSHOT"

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

tasks.register<Test>("test-common") {
    dependsOn(
        ":vadl:test-others",
        ":vadl-cli:test",
        ":vadl-lsp:test",
        ":java-annotations:test",
    )
}

tasks.register("checkstyleAll") {
    dependsOn(
        subprojects.flatMap {
            listOf("${it.path}:checkstyleMain", "${it.path}:checkstyleTest")
        },
    )
}
