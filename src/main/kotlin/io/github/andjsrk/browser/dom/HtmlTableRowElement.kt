package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Tr)
interface HtmlTableRowElement: HtmlElement {
    class Impl: HtmlTableRowElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Tr)
}
