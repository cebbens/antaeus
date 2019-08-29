plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))

    implementation("io.javalin:javalin:3.4.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.9.3")
}
