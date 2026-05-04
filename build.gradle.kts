plugins {
    kotlin("jvm") version "1.9.24"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "fun.aikoxd"
version = "1.0.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveFileName.set("KatsSillyPlugin-1.0.2.jar")
    // Relocation is disabled due to a bug in Shadow plugin with Kotlin 1.9 metadata
    // relocate("net.wesjd.anvilgui", "com.kaity.server.libs.anvilgui")
}

tasks.jar {
    enabled = true
}
