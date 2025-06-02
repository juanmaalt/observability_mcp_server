plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

val ktorVersion = "3.0.1"
val mcpVersion = "0.5.0"

dependencies {
    implementation("io.ktor:ktor-client-core:3.0.1")
    implementation("io.ktor:ktor-client-cio:3.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.5.0")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("AppKt")
}
