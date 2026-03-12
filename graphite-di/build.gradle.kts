plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(21)
}
