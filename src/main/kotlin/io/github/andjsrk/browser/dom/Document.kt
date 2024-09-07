package io.github.andjsrk.browser.dom

interface Document: Node {
    enum class Type {
        Xml,
        Html,
    }
    enum class Mode {
        NoQuirks,
        Quirks,
        LimitedQuirks,
    }

    class Impl: Document, Node by Node.Impl()
}
