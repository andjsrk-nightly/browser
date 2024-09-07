package io.github.andjsrk.browser.dom

interface HtmlElement: Element {
    val type: HtmlElementType

    class Impl(override val type: HtmlElementType): HtmlElement, Element by Element.Impl()
}

inline infix fun HtmlElement.`is`(type: HtmlElementType) =
    this.type == type
inline infix fun Element.`is`(type: HtmlElementType) =
    this is HtmlElement && this `is` type
