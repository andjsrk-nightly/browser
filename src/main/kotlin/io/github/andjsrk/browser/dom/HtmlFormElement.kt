package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Form)
interface HtmlFormElement: HtmlElement {
    class Impl: HtmlFormElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Form)
}
