package com.grappenmaker.snake

import kotlin.math.abs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.sqrt

@Serializable
enum class Direction {
    @SerialName("up") UP,
    @SerialName("down") DOWN,
    @SerialName("left") LEFT,
    @SerialName("right") RIGHT
}

val Direction.opposite get() = when (this) {
    Direction.UP -> Direction.DOWN
    Direction.LEFT -> Direction.RIGHT
    Direction.DOWN -> Direction.UP
    Direction.RIGHT -> Direction.LEFT
}

val Direction.position get() = when (this) {
    Direction.UP -> Position(0, 1)
    Direction.LEFT -> Position(-1, 0)
    Direction.RIGHT -> Position(1, 0)
    Direction.DOWN -> Position(0, -1)
}

@Serializable
data class Position(val x: Int, val y: Int)

operator fun Position.plus(other: Position) = Position(x + other.x, y + other.y)
operator fun Position.minus(other: Position) = Position(x - other.x, y - other.y)
operator fun Position.plus(other: Direction) = plus(other.position)

fun Position.abs() = Position(abs(x), abs(y))
fun Position.euclideanDistanceTo(other: Position) =
    sqrt((other.x - x).toDouble().pow(2) + (other.y - y).toDouble().pow(2))

fun Position.manhattanDistanceTo(other: Position): Int {
    val (x, y) = (this - other).abs()
    return x + y
}

fun Position.isNextTo(other: Position) = abs(x - other.x) == 0 || abs(y - other.y) == 0
fun Position.adjacent() = listOf(
    this + Direction.UP,
    this + Direction.DOWN,
    this + Direction.LEFT,
    this + Direction.RIGHT
)

data class Rectangle(val a: Position, val b: Position)
fun rectFromSize(width: Int, height: Int) = Rectangle(Position(0, 0), Position(width - 1, height - 1))

operator fun Rectangle.contains(other: Position) =
    other.x >= a.x && other.y >= a.y && other.y <= b.y && other.x <= b.x