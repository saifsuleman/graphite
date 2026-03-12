plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "graphite"

include("graphite-di")
include("graphite-scanning")
include("graphite-examples")