plugins {
    kotlin("jvm") version "2.3.10"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "io.wesner.robert.cb1060.clamplace"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://maven.robert.wesner.io/repository/maven-public/")
    maven("https://maven.robert.wesner.io/repository/johnymuffin-maven-public/")
}

dependencies {
    implementation("com.legacyminecraft.poseidon:poseidon-craftbukkit:1.+")
}

kotlin {
    jvmToolchain(8)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}
