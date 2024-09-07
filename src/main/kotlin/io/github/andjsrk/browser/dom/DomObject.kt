package io.github.andjsrk.browser.dom

import io.github.andjsrk.browser.common.util.safeCastTo
import java.lang.ref.WeakReference

interface DomObject {
    var parent: WeakReference<DomObject>?
    val children: MutableList<DomObject>
    val root: DomObject get() =
        parent?.get()?.root ?: this
    val shadowIncludingRoot: DomObject get() =
        root.let {
            if (it is ShadowRoot) it.hostNotNull.shadowIncludingRoot
            else it
        }

    class Impl: DomObject {
        override var parent: WeakReference<DomObject>? = null
        override val children = mutableListOf<DomObject>()
    }
}

val DomObject.descendants: Sequence<DomObject> get() =
    children.asSequence().flatMap {
        sequence {
            yield(it)
            yieldAll(it.descendants)
        }
    }
val DomObject.inclusiveDescendants get() =
    sequenceOf(this) + descendants
val DomObject.ancestors: Sequence<DomObject> get() =
    sequence {
        parent?.get()?.let {
            yield(it)
            yieldAll(it.ancestors)
        }
    }
val DomObject.inclusiveAncestors get() =
    sequenceOf(this) + ancestors
inline val DomObject.firstChild get() =
    children.firstOrNull()
inline val DomObject.lastChild get() =
    children.lastOrNull()
val DomObject.previousSibling get() =
    parent?.get()?.children?.let { pc ->
        pc.getOrNull(pc.indexOf(this) - 1)
    }
val DomObject.nextSibling get() =
    parent?.get()?.children?.let { pc ->
        pc.getOrNull(pc.indexOf(this) + 1)
    }
val DomObject.index get() =
    indexOn(requireNotNull(parent?.get()))
fun DomObject.indexOn(parent: DomObject) =
    parent.children.indexOf(this)

val DomObject.shadowIncludingDescendants: Sequence<DomObject> get() =
    descendants.flatMap {
        sequence {
            yield(it)
            it.safeCastTo<Element>()?.shadowRoot?.let {
                yieldAll(it.shadowIncludingDescendants)
            }
        }
    }
val DomObject.shadowIncludingInclusiveDescendants get() =
    sequenceOf(this) + shadowIncludingDescendants
