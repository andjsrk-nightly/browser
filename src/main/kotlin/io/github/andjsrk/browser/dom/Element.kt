package io.github.andjsrk.browser.dom

@JvmInline
value class NamedNodeMap(val items: MutableMap<String, Attr> = mutableMapOf())
interface Element: Node {
    val attributes: NamedNodeMap
    val shadowRoot: ShadowRoot?

    class Impl: Element, Node by Node.Impl() {
        override val attributes = NamedNodeMap()
        override val shadowRoot: ShadowRoot? = null
    }
}

inline val Element.isShadowHost get() = shadowRoot != null
