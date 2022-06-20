plugins {
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
}

group = "com.grappenmaker"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // kotlinx.serialization dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // Ktor dependencies
    serverImplementation("core", "netty", "content-negotiation", "call-logging", "auto-head-response")
    ktorImplementation("serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:1.2.11")
}

fun DependencyHandlerScope.serverImplementation(vararg names: String) =
    names.forEach { ktorImplementation("server-$it") }

fun DependencyHandlerScope.ktorImplementation(name: String) = implementation(
    group = "io.ktor",
    name = "ktor-$name",
    version = "2.0.1"
)

tasks {
    withType<Jar>().configureEach {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes("Main-Class" to "com.grappenmaker.snake.Main")
        }
    }

    processResources {
        expand("version" to version, "timestamp" to System.currentTimeMillis())
    }
}