package io.github.andjsrk.browser.common

/**
 * A simple stack implementation using [MutableList].
 * Grows upwards.
 */
@JvmInline
value class ListStack<E>(
    /**
     * For convenience and performance, intentionally exposes the property.
     */
    val list: MutableList<E>,
)/* : Iterable<E> by list */ {
    constructor(): this(mutableListOf())
    constructor(vararg elements: E): this(mutableListOf(*elements))

    val size get() = list.size
    fun top() =
        list.last()
    fun topOrNull() =
        list.lastOrNull()
    fun bottom() =
        list.first()
    fun bottomOrNull() =
        list.firstOrNull()
    fun push(element: E) =
        list.add(element)
    fun pop() =
        list.removeLast()
    fun popOrNull() =
        list.removeLastOrNull()
}

fun <T> ListStack<T>.asReversed() = ListStack(list.asReversed())
