package com.grappenmaker.snake

fun <T> Iterable<Iterable<T>>.swapOrder(forceDrain: Boolean = true) = buildList {
    val iterators = this@swapOrder.map { it.iterator() }
    while (iterators.all { it.hasNext() }) {
        add(iterators.map { it.next() })
    }

    if (forceDrain && iterators.any { it.hasNext() }) error("Iterators were not drained while swapping")
}

fun <T> List<T>.asPair() = this[0] to this[1]
fun <T> Pair<T, T>.asList() = listOf(first, second)
inline fun <F, T> Pair<F, F>.mapBoth(block: (F) -> T) = block(first) to block(second)
inline fun <F, S, T> Pair<F, S>.mapFirst(block: (F) -> T) = block(first) to second
inline fun <F, S, T> Pair<F, S>.mapSecond(block: (S) -> T) = first to block(second)

fun <T> Sequence<T>.takeWhileInclusive(pred: (T) -> Boolean): Sequence<T> {
    var shouldContinue = true
    return takeWhile {
        val result = shouldContinue
        shouldContinue = pred(it)
        result
    }
}