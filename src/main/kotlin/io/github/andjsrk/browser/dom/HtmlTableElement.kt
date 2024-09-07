package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Table)
interface HtmlTableElement: HtmlElement {
    class Impl: HtmlTableElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Table)
}
