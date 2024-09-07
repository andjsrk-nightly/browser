package io.github.andjsrk.browser.dom

import io.github.andjsrk.browser.common.util.safeCastTo

sealed class Slottable(node: Node): Node by node {
    companion object {
        operator fun invoke(element: Element) =
            ElementV(element)
        operator fun invoke(text: Text) =
            TextV(text)
    }

    // to avoid name conflicts, put 'V' which stands for 'variant'
    class ElementV(val element: Element): Slottable(element)
    class TextV(val text: Text): Slottable(text)

    var slotName: String = ""
    var assignedSlot: HtmlSlotElement? = null
    val manualSlotAssignment: Unit? = null
}

fun Slottable.findSlot(open: Boolean = false): HtmlSlotElement? {
    val nodeThis = this as Node
    val parent = nodeThis.parent?.get() ?: return null
    val shadow = parent.safeCastTo<Element>()?.shadowRoot ?: return null
    if (open && shadow.mode != ShadowRoot.Mode.Open) return null
    if (shadow.slotAssignment == ShadowRoot.SlotAssignmentMode.Manual) {
        return shadow.descendants
            .filterIsInstance<HtmlSlotElement>()
            .firstOrNull { this in it.manuallyAssignedNodes }
    }
    return shadow.descendants
        .filterIsInstance<HtmlSlotElement>()
        .firstOrNull { it.name == slotName }
}

fun Slottable.assignSlot() {
    findSlot()?.assignSlottables()
}

inline val Slottable.assigned get() =
    assignedSlot != null
