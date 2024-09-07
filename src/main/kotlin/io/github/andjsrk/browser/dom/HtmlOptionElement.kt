package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Option)
interface HtmlOptionElement: HtmlElement {
    class Impl: HtmlOptionElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Option)
}
