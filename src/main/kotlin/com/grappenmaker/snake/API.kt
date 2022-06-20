package com.grappenmaker.snake

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.system.measureTimeMillis

fun Application.setupRouting(configuration: Configuration) = routing {
    get("/") {
        call.respond(BotInfoRequest(
            author = author,
            color = configuration.color,
            head = configuration.head,
            tail = configuration.tail,
            version = Versions.version
        ))
    }

    post("/start") {
        // Start a game, I guess. We should probably do something with this later, but right now,
        // I don't really care that much about the game start. We will see later...
        call.respond(HttpStatusCode.OK)
    }

    post("/move") {
        val moveRequest = call.receive<MoveRequest>()
        val taken = measureTimeMillis { call.respond(makeMove(moveRequest)) }
        if (taken > 100) {
            println("Turn ${moveRequest.turn} took way too long to process! (${taken}ms)")
        }
    }

    post("/end") {
        // Again, we don't really care that much here...
        call.respond(HttpStatusCode.OK)
    }
}