package io.github.andjsrk.browser.dom

interface ShadowRoot: DocumentFragment {
    enum class Mode {
        Open,
        Closed,
    }
    enum class SlotAssignmentMode {
        Manual,
        Named,
    }

    val mode: Mode
    val delegatesFocus: Boolean
    val availableToElementInternals: Boolean
    val declarative: Boolean
    val slotAssignment: SlotAssignmentMode
    val clonable: Boolean
    val serializable: Boolean

    class Impl(
        override val mode: Mode,
        override val slotAssignment: SlotAssignmentMode,
        host: Element,
        override val declarative: Boolean = false,
    ): ShadowRoot, DocumentFragment by DocumentFragment.Impl() {
        init {
            hostNotNull = host
        }
        override val delegatesFocus = false
        override val availableToElementInternals = false
        override val clonable: Boolean = false
        override val serializable: Boolean = false

        override fun getParent(event: Unit): EventTarget? =
            super<ShadowRoot>.getParent(event)
    }

    override fun getParent(event: Unit): EventTarget? =
        if (false/* event's composed flag is unset and shadow root is the root of event's path's first struct's invocation target */) null
        else host
}

var ShadowRoot.hostNotNull
    get() = host!!
    set(value) {
        host = value
    }
