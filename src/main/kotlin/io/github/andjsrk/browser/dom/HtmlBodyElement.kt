package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Body)
interface HtmlBodyElement: HtmlElement {
    class Impl: HtmlBodyElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Body)
}
