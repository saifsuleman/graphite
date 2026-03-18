plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":graphite-di"))
    implementation(project(":graphite-scanning"))
}

kotlin {
    jvmToolchain(21)
}
