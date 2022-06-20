package com.grappenmaker.snake

fun makeMove(request: MoveRequest): MoveResponse {
    // This is where the magic happens...
    val (_, _, board, me) = request

    // Filter out all impossible moves
    val possibilities = enumValues<Direction>().filter { move -> board.boundsCheck(me.head + move) }

    // Find the closest food for later using BFS
    val closestFood = board.bestFood(me.head)

    // Find all snakes that we might bump into the next move;
    // We may want to avoid those snakes
    val (scary, killable) = request.bumpableSnakes()

    // Find the best move out of all possible ones
    // using a scoring function
    val move = possibilities.maxByOrNull { move ->
        // Calculate the next intended position for this move type
        val newPosition = me.head + move

        // Calculate the amount of board space left
        val spaceLeft = request.areaControl(newPosition)

        // If there will be not enough space left, this is probably
        // an awful move to do
        val spacePenalty = if (spaceLeft <= me.length) 1000 else 0

        // Calculate food score based on distance to closest food
        val foodScore = -(closestFood?.manhattanDistanceTo(newPosition) ?: 0)

        // Define a function to determine if we could bump into
        // a given snake the next tick with the new calculated position
        val isBumpable = { other: BattleSnake -> newPosition.isNextTo(other.head) }

        // Is it a bad thing? Then this is also probably a really bad mpev
        val hasScary = scary.any(isBumpable)
        val scaryScore = if (hasScary) -10000 else 0 // we really don't want to bump into scary snakes

        // If they can be killed the next tick, add a little bit of extra score to "motivate"
        // a kill by our snake
        val hasKillable = killable.any(isBumpable)
        val killScore = if (hasKillable && spaceLeft > me.length) 10 else -10

        // Find out if we are hungry and therefore set a multiplier
        val foodMultiplier = if (me.health < 50) 1.5 else 0.5

        // Find the best move by summing the scores
        foodScore * foodMultiplier + spaceLeft * 10 + scaryScore + killScore - spacePenalty
    } ?: Direction.DOWN // Defaulting to "down"

    // Send back the response to the server
    return MoveResponse("I am moving ${move.name.lowercase()}!", move)
}

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
fun Board.bestFood(position: Position) = bfs(
    initial = position,
    condition = { it in food },
    searcher = { curr -> curr.adjacent().filter { boundsCheck(it) } }
)

// Flood fill the board based on if the next spot is valid
// TODO: keep track of snake movement (they grow and shrink)
// Returns the amount of squares we have control over
fun MoveRequest.areaControl(position: Position) = floodFill(
    initial = position,
    condition = { next ->
        val inBounds = board.boundsCheck(next)
        val controlling = board.controllingSnake(position) == you
        val (scary) = bumpableSnakes(next)
        inBounds && controlling && scary.isEmpty()
    },
    searcher = { next -> next.adjacent() }
).size

// Returns what snake can reach a given position the fastest
// Null when there are no snakes on the board
fun Board.controllingSnake(position: Position): BattleSnake? {
    val controllers = snakes.sortedBy { it.head.manhattanDistanceTo(position) }
    val closest = controllers.firstOrNull() ?: return null
    val best = closest.head.manhattanDistanceTo(position)

    return controllers
        .takeWhile { it.head.manhattanDistanceTo(position) == best }
        .maxByOrNull { it.length }
}

// Check if a position is a valid position
// That is, the position is inside of the board bounds,
// Not inside a snake body except a tail,
// And the position is not in a hazard
fun Board.boundsCheck(position: Position) =
    position in rectFromSize(width, height) &&
            snakes.none { position in it.body.dropLast(1) } &&
            position !in hazards