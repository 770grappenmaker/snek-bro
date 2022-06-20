package com.grappenmaker.snake

class SearchContext<T>(
    initial: T,
    val queue: ArrayDeque<T> = ArrayDeque(),
    val seen: HashSet<T> = hashSetOf()
) {
    init {
        process(initial)
    }

    fun process(element: T) {
        if (seen.add(element)) queue.add(element)
    }
}

// Performs BFS
inline fun <T : Any> bfs(
    // Initial value for the search
    initial: T,
    // Condition for the "end" case (if this yields true, the function will return)
    condition: SearchContext<T>.(T) -> Boolean,
    // from current element to list of next candidates
    searcher: SearchContext<T>.(T) -> List<T>
): T? {
    val context = SearchContext(initial)
    with(context) {
        while (queue.isNotEmpty()) {
            val next = queue.removeLast()
            if (condition(next)) return next
            else searcher(next).forEach { process(it) }
        }
    }

    return null
}

// Performs floodfill, using a condition
// Similar to BFS except the result is a set
inline fun <T : Any> floodFill(
    // Initial value for the fill
    initial: T,
    // Condition to continue visiting
    condition: SearchContext<T>.(T) -> Boolean,
    // from current element to list of next candidates
    searcher: SearchContext<T>.(T) -> List<T>
) = buildSet {
    with(SearchContext(initial)) {
        while (queue.isNotEmpty()) {
            val next = queue.removeLast()
            if (condition(next) && add(next)) {
                searcher(next).forEach { process(it) }
            }
        }
    }
}