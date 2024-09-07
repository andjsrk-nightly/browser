package io.github.andjsrk.browser.dom

@UsedByElements(HtmlElementType.Template)
interface HtmlTemplateElement: HtmlElement {
    val contents: DocumentFragment

    class Impl: HtmlTemplateElement, HtmlElement by HtmlElement.Impl(HtmlElementType.Template) {
        override val contents = DocumentFragment.Impl()
    }
}
