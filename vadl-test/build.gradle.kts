plugins {
    alias(libs.plugins.conventions.java)
}

sourceSets {
    test {
        resources.setSrcDirs(listOf("resources"))
    }
}

