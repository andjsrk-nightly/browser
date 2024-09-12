package io.github.andjsrk.browser.html.parsing

import io.github.andjsrk.browser.common.*
import io.github.andjsrk.browser.common.util.lazyDropBefore
import io.github.andjsrk.browser.common.util.previousOf
import io.github.andjsrk.browser.dom.*

class ParseState {
    var insertionMode = InsertionMode.Initial
    /**
     * Note: unlike the spec, the stack grows upwards.
     */
    val openElements = ListStack<Element>()
    val activeFormattingElements = mutableListOf<Element>()
    val headElementPointer: HtmlHeadElement? = null
    val formElementPointer: HtmlFormElement? = null
    val templateInsertionModes = ListStack<InsertionMode>()

    fun resetInsertionModeAppropriately() {
        var last = false
        var node = openElements.top()

        while (true) {
            if (node === openElements.bottom()) {
                last = true
                if (false/* TODO: the parser was created as part of the HTML fragment parsing algorithm (fragment case) */) {
                    node = TODO() // the context element passed to that algorithm
                }
            }

            insertionMode = when {
                node is HtmlSelectElement -> run mode@ {
                    if (!last) {
                        val ancestors = openElements
                            .asReversed()
                            .list
                            .lazyDropBefore(node)
                            .drop(1)

                        for (ancestor in ancestors) {
                            when (ancestor) {
                                is HtmlTemplateElement -> break
                                is HtmlTableElement -> return@mode InsertionMode.InSelectInTable
                            }
                        }
                    }

                    InsertionMode.InSelect
                }
                node is HtmlTableCellElement && !last -> InsertionMode.InCell
                node is HtmlTableRowElement -> InsertionMode.InRow
                node is HtmlTableSectionElement -> InsertionMode.InTableBody
                node is HtmlTableCaptionElement -> InsertionMode.InCaption
                node `is` HtmlElementType.Colgroup -> InsertionMode.InColumnGroup
                node is HtmlTableElement -> InsertionMode.InTable
                node is HtmlTemplateElement -> templateInsertionModes.top()
                node is HtmlHeadElement -> InsertionMode.InHead
                node is HtmlBodyElement -> InsertionMode.InBody
                node is HtmlFrameSetElement -> InsertionMode.InFrameset
                node is HtmlHtmlElement ->
                    if (headElementPointer == null) InsertionMode.BeforeHead
                    else InsertionMode.AfterHead
                last -> InsertionMode.InBody
                else -> {
                    node = openElements.list.previousOf(node)!!
                    continue
                }
            }
            break
        }
    }
    fun currentNode() =
        openElements.topOrNull()
    fun adjustedCurrentNode() =
        if (false/* TODO: the parser was created as part of the HTML fragment parsing algorithm */ && openElements.size == 1) TODO() // the context element
        else currentNode()
}
