package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Select)
interface HtmlSelectElement: HtmlElement {
    val options: MutableList<HtmlOptionElement>

    class Impl: HtmlSelectElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Select) {
        override val options = mutableListOf<HtmlOptionElement>()
    }
}
