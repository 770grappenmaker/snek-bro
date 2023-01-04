package com.grappenmaker.snake

import com.grappenmaker.snake.Direction.*

fun GameRequest.move() = GameState(board, you, game, board.snakes.size == 1).preorderSnakes().best()
fun GameState.preorderSnakes() = copy(board = board.copy(snakes = listOf(you) + (board.snakes - you)))

data class GameState(val board: Board, val you: Snake, val game: Game, val singlePlayer: Boolean) {
    val controlledSquares by lazy { board.areaControl() }
}

val scoreComparator = compareBy<GameScore> { it.winScore }
    .thenByDescending { it.foodDist }
    .thenBy { it.room }

data class GameScore(
    val room: Int,
    val foodDist: Int,
    val winScore: Int,
) : Comparable<GameScore> {
    override fun compareTo(other: GameScore) = scoreComparator.compare(this, other)
}

operator fun GameScore.unaryMinus() = GameScore(-room, -foodDist, -winScore)

fun GameState.scoreFor(snake: Snake): GameScore {
    val alive = board.aliveSnakes
//    val areaControl = controlledSquares.count { it.value == snake }
    val areaControl = controlledSquares[snake] ?: 0

    return when {
        // if dead, worst move ever
        snake.isDead -> GameScore(areaControl, 0, Int.MIN_VALUE)
        // if winner, best move ever
        !singlePlayer && alive.singleOrNull() == snake -> GameScore(areaControl, 0, Int.MAX_VALUE)
        // if measuring our snake, make sure to grab some food
        snake == you && snake.health < 30 -> GameScore(
            room = areaControl,
            foodDist = board.food.minOf { it manhattanDistanceTo snake.head },
            winScore = 0
        )

        else -> GameScore(areaControl, 0, 0)
    }
}

fun GameState.allScores() = board.snakes.map { scoreFor(it) }
fun GameState.ourScore() = scoreFor(you)

// Assumes snakes are pre-ordered with the preorderSnakes function
fun GameState.best(depth: Int = 3): Direction {
    val (bestMove) = you.decisionSequence(this).maxBy { (_, s) -> singleMove(0, s).search(depth, 1)[0] }
    val newLoc = you.head + bestMove
    return if (newLoc !in board) bestHeuristic() else bestMove
}

fun GameState.search(depth: Int, snakeIdx: Int): List<GameScore> = when {
    depth == 0 -> allScores()
    snakeIdx >= board.snakes.size -> search(depth - 1, 0)
    board.snakes[snakeIdx].isDead -> search(depth, snakeIdx + 1)
    else -> board.snakes[snakeIdx].decisionSequence(this)
        .map { (_, s) -> singleMove(snakeIdx, s).search(depth, snakeIdx + 1) }
        .maxBy { it[snakeIdx] }
}

fun GameState.bestHeuristic() = you.decisionSequence(this).maxBy { (_, s) -> singleMove(0, s).ourScore() }.first

//fun GameState.search(
//    depth: Int,
//    snakeIdx: Int,
//    alpha: GameScore = worstScore,
//    beta: GameScore = bestScore
//): GameScore = when {
//    // if we reached terminal depth, stop and evaluate
//    depth == 0 -> ourScore()
//    // if we have seen all snakes, restart our search and go a layer deeper
//    snakeIdx >= board.snakes.size -> search(depth - 1, 0, alpha, beta)
//    // if the currently visited snake is dead, just continue
//    board.snakes[snakeIdx].isDead -> search(depth, snakeIdx + 1, alpha, beta)
//    else -> {
//        var currAlpha = alpha
//        var currBeta = beta
//
//        // requires that the snakes are pre-sorted with our snake in index 0
//        val maximizing = snakeIdx == 0
//
//        buildList {
//            for (newState in board.snakes[snakeIdx].decisionSequence(this@search)
//                .map { (_, s) -> singleMove(snakeIdx, s) }
//                .sortedBy { if (maximizing) -it.ourScore() else it.ourScore() }
//            ) {
//                val score = newState.search(depth, snakeIdx + 1, currAlpha, currBeta)
//                add(score)
//                if (maximizing) {
//                    if (score > currBeta) break
//                    currAlpha = maxOf(currAlpha, score)
//                } else {
//                    if (score < currAlpha) break
//                    currBeta = maxOf(currBeta, score)
//                }
//            }
//        }.maxBy { if (maximizing) it else -it }
//
////        // Find all possible next states for the current snake
////        board.snakes[snakeIdx].decisionSequence(this)
////            // step the board according to those decisions
////            .map { (_, s) -> singleMove(snakeIdx, s) }
////            // sort all boards so pruning is more efficient
////            .sortedBy { if (maximizing) -it.ourScore() else it.ourScore() }
////            // recursively find the best evaluation
////            .map { it.search(depth, snakeIdx + 1, currAlpha, currBeta) }
////            // continue until prune
////            .takeWhileInclusive { if (maximizing) it <= currBeta else it >= currAlpha }
////            // update alpha/beta values every time
////            .onEach { if (maximizing) currAlpha = max(currAlpha, it) else currBeta = min(currBeta, it) }
////            // find the best move for the current player
////            .maxBy { if (maximizing) it else -it }
//    }
//}

fun Snake.decisionSequence(state: GameState) =
    if (isDead) emptySequence() else enumValues<Direction>().asSequence()
        .filter { d -> head + d != body[1] }
        .map { d -> d to step(d, state) }

fun GameState.singleMove(idx: Int, newSnake: Snake) = copy(
    board = board.singleMove(idx, newSnake),
    you = if (idx == 0) newSnake else you
)

fun Board.singleMove(idx: Int, newSnake: Snake) = update(snakes.toMutableList().also { it[idx] = newSnake })

fun Board.update(snakes: List<Snake>): Board {
    val aliveSnakes = snakes.filter { it.isAlive }
    val tails = aliveSnakes.flatMap { it.body.drop(1) }.toSet()
    val updatedSnakes = snakes.map {
        when {
            it.isDead || (aliveSnakes - it).any { other ->
                (other.body.size >= it.body.size && it.head == other.head) || it.head in tails
            } -> it.copy(health = 0)

            else -> it
        }
    }

    val heads = aliveSnakes.map { it.head }.toSet()
    return copy(food = food.filterNot { it in heads }, snakes = updatedSnakes)
}

fun Snake.step(direction: Direction, state: GameState): Snake {
    val newHeadPos = head + direction
    val newHead = when {
        state.game.isWrapped -> Point(newHeadPos.x.mod(state.board.width), newHeadPos.y.mod(state.board.height))
        else -> newHeadPos
    }

    val food = newHead in state.board.food
    val newTail = if (state.game.isConstrictor || food) body else body.dropLast(1)
    val newBody = listOf(newHead) + newTail
    return copy(
        head = newHead,
        health = when {
            newHead !in state.board || newHead in newTail -> 0
            food -> 100
            else -> (health - (if (newHead in state.board.hazards) state.game.hazardDamage else 0) - 1).coerceIn(0..100)
        },
        body = newBody
    )
}

fun Board.areaControl(): Map<Snake, Int> {
    val liveSnakes = aliveSnakes
    val illegal = hazards.toSet() + liveSnakes.flatMap { it.body }.toSet()
    val legal = sizedRect(width, height).points.filter { it !in illegal }
    val d = listOf(UP, DOWN, LEFT, RIGHT)
    return liveSnakes.associateWith { snake ->
        floodFill(snake.head, neighbors = { p ->
            d.map { p + it }.filter { it in legal }
//            p.adjacent(this).filter { a -> a !in illegal }
        }).size
    }
}

//fun Board.areaControl(): Map<Snake, Int> {
//    val liveSnakes = aliveSnakes.sortedByDescending { it.body.size }
//
//    data class SearchEntry(val snake: Snake, val pos: Point)
//
//    val queue = queueOf(liveSnakes.map { SearchEntry(it, it.head) })
////    val result = mutableMapOf<Point, Snake>()
//    val result = liveSnakes.associateWith { 0 }.toMutableMap()
//    val seen = hashSetOf<Point>()
//    val illegal = hazards.toSet() + liveSnakes.flatMap { it.body }.toSet()
//    queue.drain { (snake, pos) ->
////        result.putIfAbsent(pos, snake)
//        result[snake] = result.getValue(snake) + 1
//        pos.adjacent(this@areaControl)
//            .forEach { if (seen.add(it) && it !in illegal) queue.addFirst(SearchEntry(snake, it)) }
//    }
//
//    return result
//}

//fun Board.areaControl(): Map<Point, Snake?> {
//    val liveSnakes = aliveSnakes
//    val heads = liveSnakes.map { it.head }
//    val illegal = liveSnakes.flatMap { it.body.drop(1) }.toSet() + hazards.toSet()
//    val sorted = liveSnakes.sortedBy { it.body.size }
//    return (rect().points - illegal).associateWith { p ->
//        bfs(
//            initial = p,
//            isEnd = { it in heads },
//            neighbors = { curr -> curr.adjacent(this).filterNot { it in illegal } }
//        ).end?.let { sorted.first { s -> s.head == it } }
//    }
//}

fun Point.adjacent(board: Board) = adjacentSides(board.width, board.height)
fun Point.validAdjacent(board: Board) = adjacent(board).filter { p -> p in board && board.snakes.none { p in it.body } }