plugins {
    id("conventions-jvm")
}

dependencies {
    implementation(project(":vadl"))
    implementation(libs.klsp)

    testImplementation(libs.assertj.core)
}
