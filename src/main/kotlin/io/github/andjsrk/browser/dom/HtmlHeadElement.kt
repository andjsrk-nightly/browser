package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Head)
interface HtmlHeadElement: HtmlElement {
    class Impl: HtmlHeadElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Head)
}
