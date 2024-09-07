package io.github.andjsrk.browser.dom

interface DocumentFragment: Node {
    var host: Element?

    class Impl: DocumentFragment, Node by Node.Impl() {
        override var host: Element? = null
    }
}
