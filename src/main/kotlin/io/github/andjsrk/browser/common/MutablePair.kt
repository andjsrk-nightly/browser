package io.github.andjsrk.browser.common

data class MutablePair<A, B>(var first: A, var second: B) {
    override fun toString() = "($first, $second)"
    fun toPair() = first to second
}
