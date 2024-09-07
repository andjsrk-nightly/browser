package io.github.andjsrk.browser.dom

import io.github.andjsrk.browser.common.util.*
import java.lang.ref.WeakReference

interface Node: EventTarget {
    var nodeDocument: WeakReference<Document>

    class Impl: Node, EventTarget by EventTarget.Impl() {
        override lateinit var nodeDocument: WeakReference<Document>
        override fun getParent(event: Unit): EventTarget? {
            return super<Node>.getParent(event)
        }
    }

    override fun getParent(event: Unit): EventTarget? =
        safeCastTo<Slottable>()?.assignedSlot ?: parent?.get() as EventTarget?

    fun assignSlottablesForTree(slots: Sequence<HtmlSlotElement>? = null) {
        val slots = slots ?: inclusiveDescendants.filterIsInstance<HtmlSlotElement>()
        slots.forEach { it.assignSlottables() }
    }

    fun insertInto(parent: Node, before: Node? = null, suppressObservers: Boolean = false) {
        val nodes =
            if (this is DocumentFragment) children
            else listOf(this)
        val count = nodes.size
        if (count == 0) return
        if (this is DocumentFragment) {
            remove(true)
            // TODO: queue a tree mutation record for node with « », nodes, null, and null
        }
        if (before != null) {
            // TODO: update live ranges
        }
        val prevSibling = before?.previousSibling ?: parent.lastChild
        nodes.forEach { n ->
            adoptInto(parent.nodeDocument)
            if (before == null) parent.children += n
            else parent.children.add(before.indexOn(parent), n)
            if (parent.ifCastSuccess<Element> { it.shadowRoot?.slotAssignment == ShadowRoot.SlotAssignmentMode.Named } && n is Slottable) {
                n.assignSlot()
            }
        }
    }
    fun remove(suppressObservers: Boolean = false) {
        val parent = parent?.get()
        requireNotNull(parent)
        val index = index
        // TODO: update live ranges
        // TODO: For each NodeIterator object iterator whose root's node document is node's node document, run the NodeIterator pre-removing steps given node and iterator
        val oldPrevSibling = previousSibling
        val oldNextSibling = nextSibling
        parent.children.remove(this)
        safeCastTo<Slottable>()?.assignedSlot?.assignSlottables()
        if (parent.root is ShadowRoot && parent is HtmlSlotElement && parent.assignedNodes.isEmpty()) {
            // TODO: run signal a slot change for parent
        }
        if (inclusiveDescendants.any { it is HtmlSlotElement }) {
            parent.root.safeCastTo<Node>()?.assignSlottablesForTree()
            assignSlottablesForTree()
        }
        // TODO: Run the removing steps with node and parent
        val isParentConnected = parent.safeCastTo<Node>()?.isConnected ?: false
        if (false/* node is custom */ && isParentConnected) {
            // TODO: enqueue a custom element callback reaction with node, callback name "disconnectedCallback", and an empty argument list
        }
        shadowIncludingDescendants.forEach {
            // TODO: run the removing steps with descendant and null
            if (false/* descendant is custom */ && isParentConnected) {
                // TODO: enqueue a custom element callback reaction with descendant, callback name "disconnectedCallback", and an empty argument list
            }
        }
        parent.inclusiveAncestors.forEach {
            // TODO
        }
        if (!suppressObservers) {
            // TODO: queue a tree mutation record for parent with « », « node », oldPreviousSibling, and oldNextSibling
        }
        // TODO: run the children changed steps for parent
    }
    fun adoptInto(document: WeakReference<Document>) {
        val oldDoc = nodeDocument.get()
        val parent = parent?.get()
        if (parent != null) remove()
        if (document != oldDoc) {
            val siid = shadowIncludingInclusiveDescendants.toList()
            siid.forEach {
                it.requireIs<Node>().nodeDocument = document
                if (it is Element) {
                    it.attributes.items.values.forEach {
                        it.nodeDocument = document
                    }
                }
            }
            siid.forEach {
                if (false/* is custom */) return@forEach

                // TODO: enqueue a custom element callback reaction with inclusiveDescendant, callback name "adoptedCallback", and an argument list containing oldDocument and document
            }
            siid.forEach {
                // TODO: run the adopting steps with inclusiveDescendant and oldDocument
            }
        }
    }
}

val Node.length get() =
    when (this) {
        is DocumentType, is Attr -> 0
        is CharacterData -> data.length
        else -> children.size
    }
inline val Node.isEmpty get() = length == 0

val Node.isConnected get() =
    shadowIncludingRoot is Document
