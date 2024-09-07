package io.github.andjsrk.browser.common.util

fun <T> List<T>.previousOf(element: T): T? {
    assert(element in this)

    return getOrNull(indexOf(element) - 1)
}

fun <T> List<T>.lazyDropBefore(element: T): Sequence<T> {
    assert(element in this)

    return asSequence().drop(indexOf(element))
}
