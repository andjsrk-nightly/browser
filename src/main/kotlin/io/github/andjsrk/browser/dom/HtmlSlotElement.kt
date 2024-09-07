package io.github.andjsrk.browser.dom

import io.github.andjsrk.browser.common.util.requireNotNull

@UsedByElements(HtmlElementType.Slot)
interface HtmlSlotElement: HtmlElement {
    var name: String
    val assignedNodes: MutableList<Slottable>
    val manuallyAssignedNodes: MutableList<Slottable>

    class Impl: HtmlSlotElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Slot) {
        override var name: String = ""
        override val assignedNodes = mutableListOf<Slottable>()
        override val manuallyAssignedNodes = mutableListOf<Slottable>()
    }

    fun findSlottables(): List<Slottable> {
        val root = root as? ShadowRoot ?: return emptyList()
        val host = root.host.requireNotNull()

        return (
            if (root.slotAssignment == ShadowRoot.SlotAssignmentMode.Manual) {
                manuallyAssignedNodes.filter { (it as Node).parent?.get() == host }
            } else {
                host.children.asSequence()
                    .filterIsInstance<Slottable>()
                    .filter { it.findSlot() == this }
                    .toMutableList()
            }
        )
    }
    fun assignSlottables() {
        val slottables = findSlottables()
        if (slottables != assignedNodes) {
            // TODO: run signal a slot change for slot
        }
        assignedNodes.clear()
        assignedNodes.addAll(slottables)
        slottables.forEach { it.assignedSlot = this }
    }
}
