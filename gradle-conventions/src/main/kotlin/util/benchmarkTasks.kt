package util

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

fun Project.registerBenchmarkTestTask(
    name: String,
    description: String,
    includePattern: String,
): TaskProvider<Test> {
    val sourceSets = extensions.getByType<SourceSetContainer>()

    return tasks.register<Test>(name) {
        val testSourceSet = sourceSets.getByName("test")
        group = "verification"
        failFast = true
        this.description = description
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        filter {
            includeTestsMatching(includePattern)
        }
        useJUnitPlatform {
            includeTags("BenchmarkTest")
        }
    }
}

fun Project.registerBenchmarkSuiteTask(
    name: String,
    description: String,
    dependencies: List<Task>,
): TaskProvider<Task> {
    return tasks.register(name) {
        group = "verification"
        this.description = description
        dependsOn(dependencies)
    }
}
