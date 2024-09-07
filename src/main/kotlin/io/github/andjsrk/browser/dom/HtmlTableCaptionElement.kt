package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Caption)
interface HtmlTableCaptionElement: HtmlElement {
    class Impl: HtmlTableCaptionElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Caption)
}
