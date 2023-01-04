package com.grappenmaker.snake

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

val snakeVersion by lazy {
    LoaderDummy::class.java.classLoader.getResourceAsStream("version.txt")?.readBytes()?.decodeToString() ?: "unknown"
}

private object LoaderDummy

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 80
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        install(CallLogging) { level = Level.INFO }

        routing {
            get("/") { call.respond(SnakeInfo()) }
            post("/start") { call.respond(HttpStatusCode.OK) }
            post("/end") { call.respond(HttpStatusCode.OK) }
            post("/move") {
                call.respond(MoveResponse("Hello, world!", call.receive<GameRequest>().move().toAPIDirection()))
            }
        }
    }.start(wait = true)
}