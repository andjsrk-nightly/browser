package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Html)
interface HtmlHtmlElement: HtmlElement {
    class Impl: HtmlHtmlElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Html)
}
