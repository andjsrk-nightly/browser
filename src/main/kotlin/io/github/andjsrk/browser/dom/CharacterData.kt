package io.github.andjsrk.browser.dom

interface CharacterData: Node {
    var data: String

    class Impl(override var data: String): CharacterData, Node by Node.Impl()

    fun replaceData(offset: Int, count: Int, data: String) {
        var count = count
        val len = length
        if (offset > len) return // TODO: throw an "IndexSizeError" DOMException
        if (offset + count > len) count = len - offset
        // TODO: queue a mutation record of "characterData" for node with null, null, node's data, <<>>, <<>>, null and null
        this.data = data.replaceRange(offset, offset + count, data)
        // TODO: update live ranges
        if (parent != null) {
            // TODO: run the children changed steps for node's parent
        }
    }
}
