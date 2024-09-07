package io.github.andjsrk.browser.dom

interface DocumentType: Node {
    class Impl: DocumentType, Node by Node.Impl()
}
