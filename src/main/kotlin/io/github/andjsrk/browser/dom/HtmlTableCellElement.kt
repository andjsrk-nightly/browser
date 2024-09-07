package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Th, HtmlElementType.Td)
interface HtmlTableCellElement: HtmlElement {
    var colSpan: Long
    var rowSpan: Long

    class Impl(type: HtmlElementType): HtmlTableCellElement, HtmlElement by HtmlElement.Impl(type) {
        override var colSpan = 0L
        override var rowSpan = 0L
    }
}
