package io.github.andjsrk.browser.html.parsing

sealed class Token {
    data class Doctype(
        var name: String? = null,
        var publicIdentifier: String? = null,
        var systemIdentifier: String? = null,
        var forceQuirks: Boolean = false,
    ): Token()

    sealed class Tag(val data: Data): Token() {
        data class Data(
            var tagName: String = "",
            val attributes: MutableList<Pair<String, String>> = mutableListOf(),
            var selfClosing: Boolean = false,
        )
    }
    class StartTag(tag: Data = Data()): Tag(tag)
    class EndTag(tag: Data = Data()): Tag(tag)
    class Comment(var data: String = ""): Token()
    class Character(var data: String): Token() {
        constructor(char: Char): this(char.toString())
    }
    object EndOfFile: Token()
}
