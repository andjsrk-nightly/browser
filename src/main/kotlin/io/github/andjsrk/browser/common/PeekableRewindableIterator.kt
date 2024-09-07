package io.github.andjsrk.browser.common

import java.util.LinkedList

open class PeekableRewindableIterator<T>(private val origin: Iterator<T>): Iterator<T> {
    private val reserved = LinkedList<T>()
    override fun hasNext() =
        reserved.isNotEmpty() || origin.hasNext()
    override fun next() =
        reserved.pollFirst() ?: origin.next()
    fun nextOrNull(): T? =
        if (hasNext()) next()
        else null
    open fun peek(index: Int) =
        reserved.getOrNull(index) ?: run {
            val indexInCollected = index - reserved.size
            val collected = mutableListOf<T>()
            for (i in 0..indexInCollected) {
                if (!origin.hasNext()) break
                collected += origin.next()
            }
            reserved += collected
            collected.getOrNull(indexInCollected)
            // here, getOrNull() is used instead of last()
            // because it might return unexpected value if the iterator have not yielded enough number of value
        }
    fun rewind(value: T?) {
        if (value == null) return // just discard the value

        reserved.addFirst(value)
    }
}
