package io.github.andjsrk.browser.dom

data class EventListener(
    val type: String,
    val callback: Unit?, // temp
    val capture: Boolean = false,
    val passive: Boolean? = null,
    val once: Boolean = false,
    val signal: Unit? = null, // temp
) {
    var removed = false
}

interface EventTarget: DomObject {
    val eventListeners: MutableList<EventListener>

    class Impl: EventTarget, DomObject by DomObject.Impl() {
        override val eventListeners = mutableListOf<EventListener>()
    }

    fun getParent(event: Unit): EventTarget? = null
}
