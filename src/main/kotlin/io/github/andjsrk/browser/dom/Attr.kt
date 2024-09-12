package io.github.andjsrk.browser.dom

interface Attr: Node {
    val namespace: String?
    val namespacePrefix: String?
    var localName: String
    var value: String
    val element: Element?

    class Impl(
        override var localName: String,
        override var value: String,
        override val element: Element?,
        override val namespace: String? = null,
        override val namespacePrefix: String? = null,
    ): Attr, Node by Node.Impl()
}
