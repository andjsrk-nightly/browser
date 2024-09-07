package io.github.andjsrk.browser.common.util

inline fun <T> Any?.safeCastTo() = this as? T
inline fun <T> Any?.ifCastSuccess(block: (T) -> Boolean) = safeCastTo<T>()?.let(block) ?: false

inline fun <reified T> Any?.requireIs(): T {
    require(this is T)
    return this
}
