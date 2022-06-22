package com.grappenmaker.snake

import kotlin.math.round

val messages = listOf(
    "Oh my god, do you really think you will win? Pathetic",
    "GG EZ",
    "Goodbye 👋",
    "Back to the lobby my friend",
    "I didn't think anybody would be this bad",
    "It's alright, everbody makes mistakes",
    "What do you mean, \"you're toxic\"?",
    "Sorry, I don't know what to say"
)

// TODO: make sure to not go to places we cannot escape
// TODO: snake shrinks eventually...
fun makeMove(request: MoveRequest): MoveResponse {
    // This is where the magic happens...
    val (_, turn, board, me) = request

    // Find the last move we did
    val lastMove = (me.head - me.body[1]).asDirection()

    // Filter out all impossible moves
    val possibilities = enumValues<Direction>().filter { move -> board.isInBounds(me.head + move) }

    // Find the closest food for later using BFS
    val closestFood = board.closestFood(me.head)

    // Find all snakes that we might bump into the next move;
    // We may want to avoid those snakes
    val (scary, killable) = request.bumpableSnakes()

    // Find the best move out of all possible ones
    // using a scoring function
    val scoredMove = possibilities.map { move ->
        // Calculate the next intended position for this move type
        val newPosition = me.head + move

        // Calculate the amount of board space left (and how much we control)
        val (controlling, available) = request.areaControl(newPosition)
        val totalArea = controlling.size + available.size

        // Calculate food score based on distance to the closest food
        val foodScore = -(closestFood?.manhattanDistanceTo(newPosition) ?: 0)

        // Define a function to determine if we could bump into
        // a given snake the next tick with the new calculated position
        val isBumpable = { other: BattleSnake -> newPosition.isNextTo(other.head) }

        // Is it a bad thing? Then this is also probably an awful move
        val hasScary = scary.any(isBumpable)
        val scaryScore = if (hasScary) -1000 else 0 // we really don't want to bump into scary snakes

        // If they can be killed the next tick, add a bit of extra score to "motivate"
        // a kill by our snake
        val hasKillable = killable.any(isBumpable)
        val killScore = if (hasKillable && totalArea >= me.length) 10 else 0

        // Return found score
        findScore(move) {
            // Food "closeness"
            val foodWeight = if (me.health < 50) 2.0 else .5
            +Score("food", weight = foodWeight, value = foodScore)

            // How many squares we have "control" over
            +Score("areaControl", weight = 10.0, value = controlling.size)

            // How much space is left (except for area control)
            +Score("spaceLeft", weight = 5.0, value = available.size)

            // If the move is scary, we might not want to do this
            +Score("scarySnakes", weight = 1.0, value = scaryScore)

            // Extra motivation for a kill attempt
            +Score("kill", weight = 1.0, value = killScore)
        }
    }.maxByOrNull { it.totalScore }

    if (System.getenv("SNAKE_DEBUG").toBoolean()) {
        println("Current move for turn $turn: ${scoredMove?.direction} (score ${scoredMove?.totalScore})")
        println(scoredMove?.debugScore() ?: "No debug info")
        println()
    }

    val direction = scoredMove?.direction ?: lastMove ?: Direction.DOWN
    // Defaulting to the last move we did, or down when we didn't move yet (happens when stuck)

    // Determine message to send (changes every second)
    val seconds = System.currentTimeMillis() / 1000
    val message = messages[(seconds % messages.size).toInt()]

    // Send back the response to the server
    return MoveResponse(message, direction)
}

data class Score(val name: String, val weight: Double, val value: Int)
class ScoreBuilder(val direction: Direction) {
    private val scores = mutableListOf<Score>()
    operator fun Score.unaryPlus() = scores.add(this)

    private val totalWeight get() = scores.sumOf { it.weight }
    val totalScore
        get() = scores.sumOf { it.value * it.weight } / totalWeight

    fun debugScore() = scores.joinToString(separator = System.lineSeparator()) { (name, weight, value) ->
        val percentage = "${round(weight / totalWeight * 1000) / 10.0}%"
        "$name: $value (weight $weight, that's $percentage)"
    }
}

inline fun findScore(move: Direction, block: ScoreBuilder.() -> Unit) =
    ScoreBuilder(move).also(block)

// Find all bumpable snakes (e.g. deltaX + deltaY <=2)
// and partition them based on scariness
fun MoveRequest.bumpableSnakes(position: Position = you.head) = board.snakes
    .filter { it != you }
    .filter {
        val (x, y) = (position - it.head).abs()
        x + y <= 2
    }.partition { it.length >= you.length }

// Find the "best" position of food to go to
// Null when there is no food on the board or no
// valid path was found
fun Board.closestFood(position: Position) = bfs(
    initial = position,
    condition = { it in food },
    searcher = { curr -> curr.adjacent().filter { isInBounds(it) } }
)

// Flood fill the board based on if the next spot is valid
// TODO: keep track of snake movement (they grow and shrink)
// Returns the squares we have control over + the squares we can reach but not
// earlier than other snakes
fun MoveRequest.areaControl(position: Position): Pair<List<Position>, List<Position>> {
    // Create tailmap (map that contains our snake -> other snake distance map)
    // Essentially the amount of steps we can still take from this position before we bump into a tail
    // So that is also the amount of body parts we can remove when checking.
    val tailMap = buildMap {
        for (other in board.snakes) {
            aStar(position, other.head, isInBounds = board::isInBounds)
                ?.let { put(other, it.size) }
        }
    }

    // Start flood filling the board
    val queue = ArrayDeque<Position>()
    val seen = hashSetOf<Position>()
    val process = { el: Position -> if (seen.add(el)) queue.add(el) }
    process(position)

    return buildSet {
        while (queue.isNotEmpty()) {
            val next = queue.removeLast()
            val (scary) = bumpableSnakes(next)

            if (scary.isEmpty() && add(next)) {
                next.adjacent().filter { pos ->
                    board.isValidPosition(pos) &&
                            board.snakes.none { pos in it.body.dropLast(tailMap[it] ?: 1) }
                }.forEach { process(it) }
            }
        }
    }.partition { board.controllingSnake(it) == you }
}

// Returns what snake can reach a given position the fastest
// Null when there are no snakes on the board

// Path distance is calculated using a*, or just the manhattan distance if
// the position is too far away (the parameter defines it)
fun Board.controllingSnake(position: Position, maxDistance: Int = snakes.maxOf { it.length }): BattleSnake? {
    if (snakes.size in 0..1) return snakes.firstOrNull()
    val controllers = snakes.map {
        val distance = it.head.manhattanDistanceTo(position)
        val pathDistance = if (distance in 0..maxDistance) aStar(
            initial = it.head,
            target = position,
            isInBounds = this::isInBounds
        )?.size ?: Int.MAX_VALUE else distance

        it to pathDistance
    }.sortedBy { (_, dist) -> dist }

    val (_, best) = controllers.firstOrNull() ?: return null
    return controllers
        .takeWhile { (_, dist) -> dist == best }
        .map { (snake) -> snake }
        .maxByOrNull { it.length }
}

// Check if a position is a valid position
// That is, the position is inside the board bounds,
// And the position is not in a hazard
fun Board.isValidPosition(position: Position) = rectContains(width, height, position) && position !in hazards

// Check if a position is a valid position,
// and if it is not inside a snake body except a tail
fun Board.isInBounds(position: Position) =
    isValidPosition(position) && snakes.none { position in it.body.dropLast(1) }