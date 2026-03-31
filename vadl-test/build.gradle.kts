plugins {
    id("conventions-jvm")
}

group = "vadl"
version = "unspecified"

sourceSets {
    test {
        resources.setSrcDirs(listOf("resources"))
    }
}
