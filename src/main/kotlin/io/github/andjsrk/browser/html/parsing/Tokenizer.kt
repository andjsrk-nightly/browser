package io.github.andjsrk.browser.html.parsing

import io.github.andjsrk.browser.common.ASCII_ALPHA_LOWER
import io.github.andjsrk.browser.common.ASCII_ALPHA_UPPER
import io.github.andjsrk.browser.common.NULL_CHAR
import io.github.andjsrk.browser.common.PeekableRewindableIterator
import io.github.andjsrk.browser.common.REPLACEMENT_CHAR
import io.github.andjsrk.browser.common.util.asciiLowercase
import io.github.andjsrk.browser.common.util.requireIs
import io.github.andjsrk.browser.common.util.take
import java.util.LinkedList

// TODO: encoding sniffing
class Tokenizer(private val parseState: ParseState, input: String) {
    private enum class State(val action: Tokenizer.() -> Token?) {
        Data({
            when (val ch = input.nextOrNull()) {
                '&' -> {
                    setReturnStateToCurrent()
                    state = CharacterReference
                    null
                }
                '<' -> {
                    state = TagOpen
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    ch.charToken()
                }
                null -> Token.EndOfFile
                else -> ch.charToken()
            }
        }),
        Rcdata({
            when (val ch = input.nextOrNull()) {
                '&' -> {
                    setReturnStateToCurrent()
                    state = CharacterReference
                    null
                }
                '<' -> {
                    state = RcdataLessThanSign
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    REPLACEMENT_CHAR.charToken()
                }
                null -> Token.EndOfFile
                else -> ch.charToken()
            }
        }),
        Rawtext({
            when (val ch = input.nextOrNull()) {
                '<' -> {
                    state = RawtextLessThanSign
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    REPLACEMENT_CHAR.charToken()
                }
                null -> Token.EndOfFile
                else -> ch.charToken()
            }
        }),
        ScriptData({
            when (val ch = input.nextOrNull()) {
                '<' -> {
                    state = ScriptDataLessThanSign
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    REPLACEMENT_CHAR.charToken()
                }
                null -> Token.EndOfFile
                else -> ch.charToken()
            }
        }),
        Plaintext({
            when (val ch = input.nextOrNull()) {
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    REPLACEMENT_CHAR.charToken()
                }
                null -> Token.EndOfFile
                else -> ch.charToken()
            }
        }),
        TagOpen({
            when (val ch = input.nextOrNull()) {
                '!' -> {
                    state = MarkupDeclarationOpen
                    null
                }
                '/' -> {
                    state = EndTagOpen
                    null
                }
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER -> {
                    state = EndTagOpen
                    null
                }
                '?' -> {
                    // unexpected-question-mark-instead-of-tag-name parse error

                    currentToken = Token.Comment("")
                    reconsumeIn(BogusComment, ch)
                    null
                }
                null -> {
                    // eof-before-tag-name parse error

                    emitMany('<'.charToken(), Token.EndOfFile)
                }
                else -> {
                    // invalid-first-character-of-tag-name parse error

                    reconsumeIn(Data, ch)
                    '<'.charToken()
                }
            }
        }),
        EndTagOpen({
            when (val ch = input.nextOrNull()) {
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER -> {
                    currentToken = Token.EndTag()
                    reconsumeIn(TagName, ch)
                    null
                }
                '>' -> {
                    // missing-end-tag-name parse error

                    state = Data
                    null
                }
                null -> {
                    // eof-before-tag-name parse error

                    emitMany('<'.charToken(), '/'.charToken(), Token.EndOfFile)
                }
                else -> {
                    // invalid-first-character-of-tag-name parse error

                    currentToken = Token.Comment("")
                    reconsumeIn(BogusComment, ch)
                    null
                }
            }
        }),
        TagName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> {
                    state = BeforeAttributeName
                    null
                }
                '/' -> {
                    state = SelfClosingStartTag
                    null
                }
                '>' -> {
                    state = Data
                    ::currentToken.take().requireIs<Token.Tag>()
                }
                in ASCII_ALPHA_UPPER -> {
                    currentTokenAs<Token.Tag>().data.tagName += ch!!.asciiLowercase()
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Tag>().data.tagName += REPLACEMENT_CHAR
                    null
                }
                null -> {
                    // eof-in-tag parse error

                    Token.EndOfFile
                }
                else -> {
                    currentTokenAs<Token.Tag>().data.tagName += ch
                    null
                }
            }
        }),
        RcdataLessThanSign({
            when (val ch = input.nextOrNull()) {
                '/' -> {
                    tempBuffer.clear()
                    state = RcdataEndTagOpen
                    null
                }
                else -> {
                    reconsumeIn(Rcdata, ch)
                    '<'.charToken()
                }
            }
        }),
        RcdataEndTagOpen({
            when (val ch = input.nextOrNull()) {
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER -> {
                    currentToken = Token.EndTag()
                    reconsumeIn(RcdataEndTagName, ch)
                    null
                }
                else -> {
                    reconsumeIn(Rcdata, ch)
                    emitMany('<'.charToken(), '/'.charToken())
                }
            }
        }),
        RcdataEndTagName({
            val ch = input.nextOrNull()
            when {
                ch in WHITESPACES && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = BeforeAttributeName
                    null
                }
                ch == '/' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = SelfClosingStartTag
                    null
                }
                ch == '>' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = Data
                    ::currentToken.take()
                }
                ch in ASCII_ALPHA_UPPER -> {
                    requireNotNull(ch)

                    currentTokenAs<Token.Tag>().data.tagName += ch.asciiLowercase()
                    tempBuffer.append(ch)
                    null
                }
                ch in ASCII_ALPHA_LOWER -> {
                    currentTokenAs<Token.Tag>().data.tagName += ch
                    tempBuffer.append(ch)
                    null
                }
                else -> {
                    reconsumeIn(Rcdata, ch)
                    emitMany(
                        '<'.charToken(),
                        '/'.charToken(),
                        *tempBufferToCharTokens(),
                    )
                }
            }
        }),
        RawtextLessThanSign({
            when (val ch = input.nextOrNull()) {
                '/' -> {
                    tempBuffer.clear()
                    state = RawtextEndTagOpen
                    null
                }
                else -> {
                    reconsumeIn(Rawtext, ch)
                    '<'.charToken()
                }
            }
        }),
        RawtextEndTagOpen({
            when (val ch = input.nextOrNull()) {
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER -> {
                    currentToken = Token.EndTag()
                    reconsumeIn(RawtextEndTagName, ch)
                    null
                }
                else -> {
                    reconsumeIn(Rawtext, ch)
                    emitMany('<'.charToken(), '/'.charToken())
                }
            }
        }),
        RawtextEndTagName({
            val ch = input.nextOrNull()
            when {
                ch in WHITESPACES && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = BeforeAttributeName
                    null
                }
                ch == '/' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = SelfClosingStartTag
                    null
                }
                ch == '>' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = Data
                    ::currentToken.take()
                }
                ch in ASCII_ALPHA_UPPER -> {
                    requireNotNull(ch)

                    currentTokenAs<Token.EndTag>().data.tagName += ch.asciiLowercase()
                    tempBuffer.append(ch)
                    null
                }
                ch in ASCII_ALPHA_LOWER -> {
                    currentTokenAs<Token.EndTag>().data.tagName += ch
                    tempBuffer.append(ch)
                    null
                }
                else -> {
                    reconsumeIn(Rcdata, ch)
                    emitMany(
                        '<'.charToken(),
                        '/'.charToken(),
                        *tempBufferToCharTokens(),
                    )
                }
            }
        }),
        ScriptDataLessThanSign({
            when (val ch = input.nextOrNull()) {
                '/' -> {
                    tempBuffer.clear()
                    state = ScriptDataEndTagOpen
                    null
                }
                '!' -> {
                    state = ScriptDataEscapeStart
                    emitMany('<'.charToken(), '!'.charToken())
                }
                else -> {
                    reconsumeIn(ScriptData, ch)
                    '<'.charToken()
                }
            }
        }),
        ScriptDataEndTagOpen({
            when (val ch = input.nextOrNull()) {
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER -> {
                    currentToken = Token.EndTag()
                    reconsumeIn(ScriptDataEndTagName, ch)
                    null
                }
                else -> {
                    reconsumeIn(ScriptData, ch)
                    emitMany('<'.charToken(), '/'.charToken())
                }
            }
        }),
        ScriptDataEndTagName({
            val ch = input.nextOrNull()
            when {
                ch in WHITESPACES && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = BeforeAttributeName
                    null
                }
                ch == '/' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = SelfClosingStartTag
                    null
                }
                ch == '>' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    state = Data
                    ::currentToken.take()
                }
                ch in ASCII_ALPHA_UPPER -> {
                    requireNotNull(ch)

                    currentTokenAs<Token.Tag>().data.tagName += ch.asciiLowercase()
                    tempBuffer.append(ch)
                    null
                }
                ch in ASCII_ALPHA_LOWER -> {
                    currentTokenAs<Token.Tag>().data.tagName += ch
                    tempBuffer.append(ch)
                    null
                }
                else -> {
                    reconsumeIn(ScriptData, ch)
                    emitMany(
                        '<'.charToken(),
                        '/'.charToken(),
                        *tempBufferToCharTokens(),
                    )
                }
            }
        }),
        ScriptDataEscapeStart({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    state = ScriptDataEscapeStartDash
                    '-'.charToken()
                }
                else -> {
                    reconsumeIn(ScriptData, ch)
                    null
                }
            }
        }),
        ScriptDataEscapeStartDash({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    state = ScriptDataEscapedDashDash
                    '-'.charToken()
                }
                else -> {
                    reconsumeIn(ScriptData, ch)
                    null
                }
            }
        }),
        ScriptDataEscaped({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    state = ScriptDataEscapedDash
                    '-'.charToken()
                }
                '<' -> {
                    state = ScriptDataEscapedLessThanSign
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    REPLACEMENT_CHAR.charToken()
                }
                null -> {
                    // eof-in-script-html-comment-like-text parse error

                    Token.EndOfFile
                }
                else -> ch.charToken()
            }
        }),
        ScriptDataEscapedDash({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    state = ScriptDataEscapedDashDash
                    '-'.charToken()
                }
                '<' -> {
                    state = ScriptDataEscapedLessThanSign
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    state = ScriptDataEscaped
                    REPLACEMENT_CHAR.charToken()
                }
                null -> {
                    // eof-in-script-html-comment-like-text parse error

                    Token.EndOfFile
                }
                else -> {
                    state = ScriptDataEscaped
                    ch.charToken()
                }
            }
        }),
        ScriptDataEscapedDashDash,
        ScriptDataEscapedLessThanSign,
        ScriptDataEscapedEndTagOpen,
        ScriptDataEscapedEndTagName,
        ScriptDataDoubleEscapeStart,
        ScriptDataDoubleEscaped,
        ScriptDataDoubleEscapedDash,
        ScriptDataDoubleEscapedDashDash,
        ScriptDataDoubleEscapedLessThanSign,
        ScriptDataDoubleEscapeEnd,
        BeforeAttributeName,
        AttributeName,
        AfterAttributeName,
        BeforeAttributeValue,
        AttributeValueDoubleQuoted,
        AttributeValueSingleQuoted,
        AttributeValueUnquoted,
        AfterAttributeValueQuoted,
        SelfClosingStartTag,
        BogusComment,
        MarkupDeclarationOpen,
        CommentStart,
        CommentStartDash,
        Comment,
        CommentLessThanSign,
        CommentLessThanSignBang,
        CommentLessThanSignBangDash,
        CommentLessThanSignBangDashDash,
        CommentEndDash,
        CommentEnd,
        CommentEndBang,
        Doctype,
        BeforeDoctypeName,
        DoctypeName,
        AfterDoctypeName,
        AfterDoctypePublicKeyword,
        BeforeDoctypePublicIdentifier,
        DoctypePublicIdentifierDoubleQuoted,
        DoctypePublicIdentifierSingleQuoted,
        AfterDoctypePublicIdentifier,
        BetweenDoctypePublicAndSystemIdentifiers,
        AfterDoctypeSystemKeyword,
        BeforeDoctypeSystemIdentifier,
        DoctypeSystemIdentifierDoubleQuoted,
        DoctypeSystemIdentifierSingleQuoted,
        AfterDoctypeSystemIdentifier,
        BogusDoctype,
        CdataSection,
        CdataSectionBracket,
        CdataSectionEnd,
        CharacterReference,
        NamedCharacterReference,
        AmbiguousAmpersand,
        NumericCharacterReference,
        HexadecimalCharacterReferenceStart,
        DecimalCharacterReferenceStart,
        HexadecimalCharacterReference,
        DecimalCharacterReference,
        NumericCharacterReferenceEnd,
    }

    private val input = InputIterator(input)
    private val reserved = LinkedList<Token>()
    private var currentToken: Token? = null
    private var state = State.Data
    private lateinit var returnState: State
    private var tempBuffer = StringBuilder()
    private var lastEmittedStartTag: Token.StartTag? = null

    fun next(): Token =
        run t@ {
            reserved.pollFirst()?.let { return@t it }

            var t = state.action(this)
            while (t == null) t = state.action(this)
            return@t t
        }
            .also {
                // post processes
                if (it is Token.StartTag) lastEmittedStartTag = it
            }
    private inline fun <reified T: Token> currentTokenAs() =
        currentToken.requireIs<T>()
    private inline fun setReturnStateToCurrent() {
        returnState = state
    }
    private fun reconsumeIn(state: State, char: Char?) {
        this.state = state
        input.rewind(char)
    }
    private inline fun emitMany(current: Token, vararg after: Token): Token {
        reserved += after
        return current
    }
    private fun tempBufferToCharTokens() =
        tempBuffer.map(Token::Character).toTypedArray()
    private val Token.EndTag.isAppropriate get() =
        data.tagName == lastEmittedStartTag?.data?.tagName
    private fun Char.charToken() =
        Token.Character(this)
}
private class InputIterator(input: String): PeekableRewindableIterator<Char>(input.iterator()) {
    override fun next(): Char {
        var next = super.next()
        if (next == '\r' && peek(0) == '\n') next = super.next()
        return normalizeNewline(next)
    }
    override fun peek(index: Int) =
        super.peek(index)?.let(::normalizeNewline)
    fun peekAsString(maxCount: Int): String {
        val res = StringBuilder(maxCount)
        val chars = mutableListOf<Char>()

        for (i in 0 until maxCount) {
            if (!hasNext()) break

            val ch = next()
            res.append(ch)
            chars += ch
        }

        chars.asReversed().forEach { rewind(it) }

        return res.toString()
    }
}

private inline fun normalizeNewline(ch: Char) =
    when (ch) {
        '\r' -> '\n'
        else -> ch
    }

private val WHITESPACES = listOf('\t', '\n', '\u000C', ' ')
