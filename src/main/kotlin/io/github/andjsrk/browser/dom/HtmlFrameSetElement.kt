package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Frameset)
interface HtmlFrameSetElement: HtmlElement {
    class Impl: HtmlFrameSetElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Frameset)
}
