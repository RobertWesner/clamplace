plugins {
    kotlin("jvm") version "2.3.10"
}

group = "io.wesner.robert.cb1060.clamplace"
version = "1.2.2"

repositories {
    mavenCentral()
    maven("https://maven.robert.wesner.io/repository/maven-public/")
    maven("https://maven.robert.wesner.io/repository/johnymuffin-maven-public/")
}

dependencies {
    implementation("com.legacyminecraft.poseidon:poseidon-craftbukkit:1.+")
    implementation("org.betamc:kotlin-libs:1.0.0-kt2.3.0")
}

kotlin {
    jvmToolchain(8)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}
