package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Colgroup, HtmlElementType.Col)
interface HtmlTableColElement: HtmlElement {
    class Impl(type: HtmlElementType): HtmlTableColElement, HtmlElement by HtmlElement.Impl(type)
}
