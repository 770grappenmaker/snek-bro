package com.grappenmaker.snake

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SnakeInfo(
    @SerialName("apiversion")
    val apiVersion: String = "1",
    val author: String = "770grappenmaker",
    val color: String = "#52A786",
    val head: String = "cute-dragon",
    val tail: String = "dragon",
    val version: String = snakeVersion
)

@Serializable
data class GameRequest(
    val game: Game,
    val turn: Int,
    val board: Board,
    val you: Snake
)

@Serializable
data class Game(
    val id: String,
    val ruleset: Ruleset,
    val map: String,
    val timeout: Int,
    val source: String
)

val Game.hazardDamage get() = ruleset.settings.hazardDamagePerTurn
val Game.isWrapped get() = ruleset.name == "wrapped"
val Game.isConstrictor get() = ruleset.name == "constrictor"

@Serializable
data class Ruleset(val name: String, val version: String, val settings: RulesetSettings)

@Serializable
data class RulesetSettings(
    val foodSpawnChance: Int,
    val minimumFood: Int,
    val hazardDamagePerTurn: Int
)

@Serializable
data class Board(
    val width: Int,
    val height: Int,
    val food: List<Point>,
    val hazards: List<Point>,
    val snakes: List<Snake>
)

val Board.aliveSnakes get() = snakes.filter { it.health > 0 }
operator fun Board.contains(point: Point) = point.x in (0 until width) && point.y in (0 until height)

@Serializable
data class Snake(
    val id: String,
    val name: String,
    val health: Int,
    val body: List<Point>,
    val latency: String,
    val head: Point,
    val length: Int,
    val shout: String,
    val squad: String,
    val customizations: SnakeCustomizations
)

val Snake.isAlive get() = health > 0
val Snake.isDead get() = health <= 0

@Serializable
data class SnakeCustomizations(val color: String, val head: String, val tail: String)

@Serializable
data class MoveResponse(val shout: String, val move: String)

fun Direction.toAPIDirection() = name.lowercase()