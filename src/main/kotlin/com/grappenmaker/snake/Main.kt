@file:JvmName("Main")

package com.grappenmaker.snake

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.event.Level

const val author = "NotEvenJoking"

fun main() {
    val configuration = readConfiguration()
    embeddedServer(Netty, port = configuration.port) {
        install(ContentNegotiation) { json(json) }
        install(AutoHeadResponse) // For uptimerobot

        setupRouting(configuration)
    }.start(wait = true)
}