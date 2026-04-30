tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        val include = System.getProperty("tags.include")
        val exclude = System.getProperty("tags.exclude")
        val benchmarkTask = name.contains("benchmark", ignoreCase = true)

        if (include != null) {
            includeTags(include)
        } else if (!benchmarkTask) {
            excludeTags("BenchmarkTest")
        }

        if (exclude != null) {
            excludeTags(exclude)
        }
    }
}
