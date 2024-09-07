package io.github.andjsrk.browser.common.util

import kotlin.reflect.KMutableProperty0

inline fun <T> T?.requireNotNull() =
    requireNotNull(this)
inline fun <T> T?.requireNotNull(lazyMessage: () -> Any) =
    requireNotNull(this, lazyMessage)

/**
 * Gets the value and leaves `null` on the property.
 */
inline fun <T> KMutableProperty0<T?>.take(): T? =
    get().also { set(null) }
