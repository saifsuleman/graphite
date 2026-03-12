plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":graphite-di"))

    implementation("io.github.classgraph:classgraph:4.8.184")
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(21)
}
