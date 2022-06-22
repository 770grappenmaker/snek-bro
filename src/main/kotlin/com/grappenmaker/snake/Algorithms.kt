package com.grappenmaker.snake

import java.util.*
import kotlin.collections.ArrayDeque

// Performs BFS
inline fun <T : Any> bfs(
    // Initial value for the search
    initial: T,
    // Condition for the "end" case (if this yields true, the function will return)
    condition: (T) -> Boolean,
    // from current element to list of next candidates
    searcher: (T) -> List<T>
): T? {
    val queue = ArrayDeque<T>()
    val seen = hashSetOf<T>()
    val process = { el: T -> if (seen.add(el)) queue.add(el) }
    process(initial)

    while (queue.isNotEmpty()) {
        val next = queue.removeLast()
        if (condition(next)) return next
        else searcher(next).forEach { process(it) }
    }

    return null
}

// Performs A* algorithm
inline fun <T : Any> aStar(
    // Initial value for the fill
    initial: T,
    // Target function (if true the function returns)
    target: (T) -> Boolean,
    // Heuristic/cost function
    crossinline heuristic: (T) -> Int,
    // Distance function
    distance: (T, T) -> Int,
    // from current element to list of next neighbours
    neighbors: (T) -> List<T>
): List<T>? {
    // Not going to use the search context here as it would become
    // way too complicated
    val cameFrom = mutableMapOf<T, T>()
    val currentScores = mutableMapOf<T, Int>()

    val score: (T) -> Int = { currentScores[it] ?: Int.MAX_VALUE }
    val tentativeScore: (T) -> Int = { score(it) + heuristic(it) }

    val queue = PriorityQueue(Comparator.comparingInt(tentativeScore))

    queue.add(initial)
    currentScores[initial] = 0

    while (queue.isNotEmpty()) {
        val current = queue.remove()!! // Java D:
        if (target(current)) {
            // Retrieve the path based on cameFrom
            return generateSequence(current) { cameFrom[it] }
                .toList().asReversed()
        }

        neighbors(current).forEach { neighbor ->
            val tentative = score(current) + distance(current, neighbor)
            if (tentative < score(neighbor)) {
                cameFrom[neighbor] = current
                currentScores[neighbor] = tentative
                if (neighbor !in queue) queue.add(neighbor)
            }
        }
    }

    return null
}

// Same thing as above, but with positions implemented for convenience
inline fun aStar(
    initial: Position,
    target: (Position) -> Boolean,
    crossinline cost: (Position) -> Int = { it.manhattanDistanceTo(initial) },
    isInBounds: (Position) -> Boolean
) = aStar(
    initial = initial,
    target = target,
    heuristic = cost,
    distance = { a, b -> a.manhattanDistanceTo(b) },
    neighbors = { it.adjacent().filter(isInBounds) }
)

inline fun aStar(
    initial: Position,
    target: Position,
    crossinline cost: (Position) -> Int = { it.manhattanDistanceTo(initial) },
    isInBounds: (Position) -> Boolean
) = aStar(initial, { it == target }, cost, isInBounds)