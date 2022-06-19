package com.grappenmaker.snake

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable
data class Configuration(
    val port: Int = 80,
    val color: String = "#4C89C8",
    val head: String = "default",
    val tail: String = "default"
)

fun readConfiguration(file: File = File("config.json")): Configuration =
    json.decodeFromString(file.readText())