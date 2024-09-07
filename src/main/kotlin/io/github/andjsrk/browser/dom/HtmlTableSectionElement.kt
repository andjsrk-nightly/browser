package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Thead, HtmlElementType.Tbody, HtmlElementType.Tfoot)
interface HtmlTableSectionElement: HtmlElement {
    class Impl(type: HtmlElementType): HtmlTableSectionElement, HtmlElement by HtmlElement.Impl(type)
}
