package io.github.andjsrk.browser.html.parsing

import io.github.andjsrk.browser.common.ASCII_ALPHA_LOWER
import io.github.andjsrk.browser.common.ASCII_ALPHA_UPPER
import io.github.andjsrk.browser.common.MutablePair
import io.github.andjsrk.browser.common.NULL_CHAR
import io.github.andjsrk.browser.common.PeekableRewindableIterator
import io.github.andjsrk.browser.common.REPLACEMENT_CHAR
import io.github.andjsrk.browser.common.util.asciiLowercase
import io.github.andjsrk.browser.common.util.requireIs
import io.github.andjsrk.browser.common.util.take
import io.github.andjsrk.browser.html.parsing.WHITESPACES
import java.util.LinkedList

// TODO: encoding sniffing
class Tokenizer(private val parseState: ParseState, input: String) {
    private enum class State(val action: Tokenizer.() -> Token?) {
        Data({
            when (val ch = input.nextOrNull()) {
                '&' -> {
                    setReturnStateToCurrent()
                    switchState(CharacterReference)
                }
                '<' -> switchState(TagOpen)
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
                    switchState(CharacterReference)
                }
                '<' -> switchState(RcdataLessThanSign)
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
                '<' -> switchState(RawtextLessThanSign)
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
                '<' -> switchState(ScriptDataLessThanSign)
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
                '!' -> switchState(MarkupDeclarationOpen)
                '/' -> switchState(EndTagOpen)
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER ->
                    switchState(EndTagOpen)
                '?' -> {
                    // unexpected-question-mark-instead-of-tag-name parse error

                    currentToken = Token.Comment("")
                    reconsumeIn(BogusComment, ch)
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
                }
                '>' -> {
                    // missing-end-tag-name parse error

                    switchState(Data)
                }
                null -> {
                    // eof-before-tag-name parse error

                    emitMany('<'.charToken(), '/'.charToken(), Token.EndOfFile)
                }
                else -> {
                    // invalid-first-character-of-tag-name parse error

                    currentToken = Token.Comment("")
                    reconsumeIn(BogusComment, ch)
                }
            }
        }),
        TagName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES ->
                    switchState(BeforeAttributeName)
                '/' -> switchState(SelfClosingStartTag)
                '>' -> {
                    switchState(Data)
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
                    switchState(RcdataEndTagOpen)
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
                ch in WHITESPACES && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(BeforeAttributeName)
                ch == '/' && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(SelfClosingStartTag)
                ch == '>' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    switchState(Data)
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
                    switchState(RawtextEndTagOpen)
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
                ch in WHITESPACES && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(BeforeAttributeName)
                ch == '/' && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(SelfClosingStartTag)
                ch == '>' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    switchState(Data)
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
                    switchState(ScriptDataEndTagOpen)
                }
                '!' -> {
                    switchState(ScriptDataEscapeStart)
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
                ch in WHITESPACES && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(BeforeAttributeName)
                ch == '/' && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(SelfClosingStartTag)
                ch == '>' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    switchState(Data)
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
                    switchState(ScriptDataEscapeStartDash)
                    '-'.charToken()
                }
                else -> reconsumeIn(ScriptData, ch)
            }
        }),
        ScriptDataEscapeStartDash({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    switchState(ScriptDataEscapedDashDash)
                    '-'.charToken()
                }
                else -> reconsumeIn(ScriptData, ch)
            }
        }),
        ScriptDataEscaped({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    switchState(ScriptDataEscapedDash)
                    '-'.charToken()
                }
                '<' ->
                    switchState(ScriptDataEscapedLessThanSign)
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
                    switchState(ScriptDataEscapedDashDash)
                    '-'.charToken()
                }
                '<' ->
                    switchState(ScriptDataEscapedLessThanSign)
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    switchState(ScriptDataEscaped)
                    REPLACEMENT_CHAR.charToken()
                }
                null -> {
                    // eof-in-script-html-comment-like-text parse error

                    Token.EndOfFile
                }
                else -> {
                    switchState(ScriptDataEscaped)
                    ch.charToken()
                }
            }
        }),
        ScriptDataEscapedDashDash({
            when (val ch = input.nextOrNull()) {
                '-' -> '-'.charToken()
                '<' -> switchState(ScriptDataEscapedLessThanSign)
                '>' -> {
                    switchState(ScriptData)
                    '>'.charToken()
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    switchState(ScriptDataEscaped)
                    REPLACEMENT_CHAR.charToken()
                }
                null -> {
                    // eof-in-script-html-comment-like-text parse error

                    Token.EndOfFile
                }
                else -> {
                    switchState(ScriptDataEscaped)
                    ch.charToken()
                }
            }
        }),
        ScriptDataEscapedLessThanSign({
            when (val ch = input.nextOrNull()) {
                '/' -> {
                    tempBuffer.clear()
                    switchState(ScriptDataEscapedEndTagOpen)
                }
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER -> {
                    tempBuffer.clear()
                    reconsumeIn(ScriptDataDoubleEscapeStart, ch)
                    '<'.charToken()
                }
                else -> {
                    reconsumeIn(ScriptDataEscaped, ch)
                    '<'.charToken()
                }
            }
        }),
        ScriptDataEscapedEndTagOpen({
            when (val ch = input.nextOrNull()) {
                in ASCII_ALPHA_LOWER, in ASCII_ALPHA_UPPER -> {
                    currentToken = Token.EndTag()
                    reconsumeIn(ScriptDataEscapedEndTagName, ch)
                }
                else -> {
                    reconsumeIn(ScriptDataEscaped, ch)
                    emitMany('<'.charToken(), '/'.charToken())
                }
            }
        }),
        ScriptDataEscapedEndTagName({
            val ch = input.nextOrNull()
            when {
                ch in WHITESPACES && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(BeforeAttributeName)
                ch == '/' && currentTokenAs<Token.EndTag>().isAppropriate ->
                    switchState(SelfClosingStartTag)
                ch == '>' && currentTokenAs<Token.EndTag>().isAppropriate -> {
                    switchState(Data)
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
                    reconsumeIn(ScriptDataEscaped, ch)
                    emitMany('<'.charToken(), '/'.charToken(), *tempBufferToCharTokens())
                }
            }
        }),
        ScriptDataDoubleEscapeStart({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES, '/', '>' -> {
                    requireNotNull(ch)

                    state =
                        if (tempBuffer.toString() == "script") ScriptDataDoubleEscaped
                        else ScriptDataEscaped
                    ch.charToken()
                }
                in ASCII_ALPHA_UPPER -> {
                    requireNotNull(ch)

                    tempBuffer.append(ch.asciiLowercase())
                    ch.charToken()
                }
                in ASCII_ALPHA_LOWER -> {
                    requireNotNull(ch)

                    tempBuffer.append(ch)
                    ch.charToken()
                }
                else -> reconsumeIn(ScriptDataEscaped, ch)
            }
        }),
        ScriptDataDoubleEscaped({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    switchState(ScriptDataDoubleEscapedDash)
                    '-'.charToken()
                }
                '<' -> {
                    switchState(ScriptDataDoubleEscapedLessThanSign)
                    '<'.charToken()
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
        ScriptDataDoubleEscapedDash({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    switchState(ScriptDataDoubleEscapedDashDash)
                    '-'.charToken()
                }
                '<' -> {
                    switchState(ScriptDataDoubleEscapedLessThanSign)
                    '<'.charToken()
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    switchState(ScriptDataDoubleEscaped)
                    REPLACEMENT_CHAR.charToken()
                }
                null -> {
                    // eof-in-script-html-comment-like-text parse error

                    Token.EndOfFile
                }
                else -> {
                    switchState(ScriptDataDoubleEscaped)
                    ch.charToken()
                }
            }
        }),
        ScriptDataDoubleEscapedDashDash({
            when (val ch = input.nextOrNull()) {
                '-' -> '-'.charToken()
                '<' -> {
                    switchState(ScriptDataDoubleEscapedLessThanSign)
                    '<'.charToken()
                }
                '>' -> {
                    switchState(ScriptData)
                    '>'.charToken()
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    switchState(ScriptDataDoubleEscaped)
                    REPLACEMENT_CHAR.charToken()
                }
                null -> {
                    // eof-in-script-html-comment-like-text parse error

                    Token.EndOfFile
                }
                else -> {
                    switchState(ScriptDataDoubleEscaped)
                    ch.charToken()
                }
            }
        }),
        ScriptDataDoubleEscapedLessThanSign({
            when (val ch = input.nextOrNull()) {
                '/' -> {
                    tempBuffer.clear()
                    switchState(ScriptDataDoubleEscapeEnd)
                    '/'.charToken()
                }
                else -> reconsumeIn(ScriptDataDoubleEscaped, ch)
            }
        }),
        ScriptDataDoubleEscapeEnd({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES, '/', '>' -> {
                    requireNotNull(ch)

                    state =
                        if (tempBuffer.toString() == "script") ScriptDataEscaped
                        else ScriptDataDoubleEscaped
                    ch.charToken()
                }
                in ASCII_ALPHA_UPPER -> {
                    requireNotNull(ch)

                    tempBuffer.append(ch.asciiLowercase())
                    ch.charToken()
                }
                in ASCII_ALPHA_LOWER -> {
                    requireNotNull(ch)

                    tempBuffer.append(ch)
                    ch.charToken()
                }
                else -> reconsumeIn(ScriptDataDoubleEscaped, ch)
            }
        }),
        BeforeAttributeName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '/', '>', null ->
                    reconsumeIn(AfterAttributeName, ch)
                '=' -> {
                    // unexpected-equals-sign-before-attribute-name parse error

                    currentAttribute = MutablePair(ch.toString(), "")
                    switchState(AttributeName)
                }
                else -> {
                    currentAttribute = MutablePair("", "")
                    reconsumeIn(AttributeName, ch)
                }
            }
        }),
        AttributeName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES, '/', '>', null ->
                    reconsumeIn(AfterAttributeName, ch)
                '=' -> switchState(BeforeAttributeValue)
                in ASCII_ALPHA_UPPER -> {
                    currentAttribute?.first += ch.asciiLowercase()
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentAttribute?.first += REPLACEMENT_CHAR
                    null
                }
                '"', '\'', '<' -> {
                    // unexpected-character-in-the-attribute-name parse error

                    // Treat it as per the "anything else" entry below
                    currentAttribute?.first += ch
                    null
                }
                else -> {
                    currentAttribute?.first += ch
                    null
                }
            }
        }),
        AfterAttributeName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '/' -> switchState(SelfClosingStartTag)
                '=' -> switchState(BeforeAttributeValue)
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-tag parse error

                    Token.EndOfFile
                }
                else -> {
                    currentAttribute = MutablePair("", "")
                    reconsumeIn(AttributeName, ch)
                }
            }
        }),
        BeforeAttributeValue({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '"' -> switchState(AttributeValueDoubleQuoted)
                '\'' -> switchState(AttributeValueSingleQuoted)
                '>' -> {
                    // missing-attribute-value parse error

                    switchState(Data)
                    ::currentToken.take()
                }
                else -> reconsumeIn(AttributeValueUnquoted, ch)
            }
        }),
        AttributeValueDoubleQuoted({
            when (val ch = input.nextOrNull()) {
                '"' -> switchState(AfterAttributeValueQuoted)
                '&' -> {
                    setReturnStateToCurrent()
                    switchState(CharacterReference)
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentAttribute?.second += REPLACEMENT_CHAR
                    null
                }
                null -> {
                    // eof-in-tag parse error

                    Token.EndOfFile
                }
                else -> {
                    currentAttribute?.second += ch
                    null
                }
            }
        }),
        AttributeValueSingleQuoted({
            when (val ch = input.nextOrNull()) {
                '\'' -> switchState(AfterAttributeValueQuoted)
                '&' -> {
                    setReturnStateToCurrent()
                    switchState(CharacterReference)
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentAttribute?.second += REPLACEMENT_CHAR
                    null
                }
                null -> {
                    // eof-in-tag parse error

                    Token.EndOfFile
                }
                else -> {
                    currentAttribute?.second += ch
                    null
                }
            }
        }),
        AttributeValueUnquoted({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> switchState(BeforeAttributeName)
                '&' -> {
                    setReturnStateToCurrent()
                    switchState(CharacterReference)
                }
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentAttribute?.second += REPLACEMENT_CHAR
                    null
                }
                '"', '\'', '<', '=', '`' -> {
                    // unexpected-character-in-unquoted-attribute-value parse error

                    // Treat it as per the "anything else" entry below
                    currentAttribute?.second += ch
                    null
                }
                null -> {
                    // eof-in-tag parse error

                    Token.EndOfFile
                }
                else -> {
                    currentAttribute?.second += ch
                    null
                }
            }
        }),
        AfterAttributeValueQuoted({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> switchState(BeforeAttributeName)
                '/' -> switchState(SelfClosingStartTag)
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-tag parse error

                    Token.EndOfFile
                }
                else -> {
                    // missing-whitespace-between-attributes parse error

                    reconsumeIn(BeforeAttributeName, ch)
                }
            }
        }),
        SelfClosingStartTag({
            when (val ch = input.nextOrNull()) {
                '>' -> {
                    currentTokenAs<Token.Tag>().data.selfClosing = true
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-tag parse error

                    Token.EndOfFile
                }
                else -> {
                    // unexpected-solidus-in-tag parse error

                    reconsumeIn(BeforeAttributeName, ch)
                }
            }
        }),
        BogusComment({
            when (val ch = input.nextOrNull()) {
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> emitMany(::currentToken.take()!!, Token.EndOfFile)
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Comment>().data += REPLACEMENT_CHAR
                    null
                }
                else -> {
                    currentTokenAs<Token.Comment>().data += ch
                    null
                }
            }
        }),
        MarkupDeclarationOpen({
            when {
                input.peekAsString(2) == "--" -> {
                    repeat(2) { input.next() }
                    currentToken = Token.Comment()
                    switchState(CommentStart)
                }
                input.peekAsString(7).equals("DOCTYPE", ignoreCase = true) -> {
                    repeat(7) { input.next() }
                    switchState(Doctype)
                }
                input.peekAsString(7) == "[CDATA[" -> {
                    repeat(7) { input.next() }
                    val adjustedCurrNode = parseState.adjustedCurrentNode()
                    if (adjustedCurrNode != null && false/* TODO: it is not an element in the HTML namespace */) {
                        switchState(CdataSection)
                    } else {
                        // cdata-in-html-content parse error

                        currentToken = Token.Comment("[CDATA[")
                        switchState(BogusComment)
                    }
                }
                else -> {
                    // incorrectly-opened-comment parse error

                    currentToken = Token.Comment()
                    switchState(BogusComment)
                }
            }
        }),
        CommentStart({
            when (val ch = input.nextOrNull()) {
                '-' -> switchState(CommentStartDash)
                '>' -> {
                    // abrupt-closing-of-empty-comment parse error

                    switchState(Data)
                    ::currentToken.take()
                }
                else -> reconsumeIn(Comment, ch)
            }
        }),
        CommentStartDash({
            when (val ch = input.nextOrNull()) {
                '-' -> switchState(CommentEnd)
                '>' -> {
                    // abrupt-closing-of-empty-comment parse error

                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-comment parse error

                    emitMany(::currentToken.take()!!, Token.EndOfFile)
                }
                else -> {
                    currentTokenAs<Token.Comment>().data += '-'
                    reconsumeIn(Comment, ch)
                }
            }
        }),
        Comment({
            when (val ch = input.nextOrNull()) {
                '<' -> {
                    currentTokenAs<Token.Comment>().data += ch
                    switchState(CommentLessThanSign)
                }
                '-' -> switchState(CommentEndDash)
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Comment>().data += REPLACEMENT_CHAR
                    null
                }
                null -> {
                    // eof-in-comment parse error

                    emitMany(::currentToken.take()!!, Token.EndOfFile)
                }
                else -> {
                    currentTokenAs<Token.Comment>().data += ch
                    null
                }
            }
        }),
        CommentLessThanSign({
            when (val ch = input.nextOrNull()) {
                '!' -> {
                    currentTokenAs<Token.Comment>().data += ch
                    switchState(CommentLessThanSignBang)
                }
                '<' -> {
                    currentTokenAs<Token.Comment>().data += ch
                    null
                }
                else -> reconsumeIn(Comment, ch)
            }
        }),
        CommentLessThanSignBang({
            when (val ch = input.nextOrNull()) {
                '-' -> switchState(CommentLessThanSignBangDash)
                else -> reconsumeIn(Comment, ch)
            }
        }),
        CommentLessThanSignBangDash({
            when (val ch = input.nextOrNull()) {
                '-' -> switchState(CommentLessThanSignBangDashDash)
                else -> reconsumeIn(CommentEndDash, ch)
            }
        }),
        CommentLessThanSignBangDashDash({
            when (val ch = input.nextOrNull()) {
                '>', null -> reconsumeIn(CommentEnd, ch)
                else -> {
                    // nested-comment parse error

                    reconsumeIn(CommentEnd, ch)
                }
            }
        }),
        CommentEndDash({
            when (val ch = input.nextOrNull()) {
                '-' -> switchState(CommentEnd)
                null -> {
                    // eof-in-comment parse error

                    emitMany(::currentToken.take()!!, Token.EndOfFile)
                }
                else -> {
                    currentTokenAs<Token.Comment>().data += '-'
                    reconsumeIn(Comment, ch)
                }
            }
        }),
        CommentEnd({
            when (val ch = input.nextOrNull()) {
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                '!' -> switchState(CommentEndBang)
                '-' -> {
                    currentTokenAs<Token.Comment>().data += '-'
                    null
                }
                null -> {
                    // eof-in-comment parse error

                    emitMany(::currentToken.take()!!, Token.EndOfFile)
                }
                else -> {
                    currentTokenAs<Token.Comment>().data += "--"
                    reconsumeIn(Comment, ch)
                }
            }
        }),
        CommentEndBang({
            when (val ch = input.nextOrNull()) {
                '-' -> {
                    currentTokenAs<Token.Comment>().data += "--!"
                    switchState(CommentEndDash)
                }
                '>' -> {
                    // incorrectly-closed-comment parse error

                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-comment parse error

                    emitMany(::currentToken.take()!!, Token.EndOfFile)
                }
                else -> {
                    currentTokenAs<Token.Comment>().data += "--!"
                    reconsumeIn(Comment, ch)
                }
            }
        }),
        Doctype({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> switchState(BeforeDoctypeName)
                '>' -> reconsumeIn(BeforeDoctypeName, ch)
                null -> {
                    // eof-in-doctype parse error

                    emitMany(
                        Token.Doctype(forceQuirks = true),
                        Token.EndOfFile
                    )
                }
                else -> {
                    // missing-whitespace-before-doctype-name parse error

                    reconsumeIn(BeforeDoctypeName, ch)
                }
            }
        }),
        BeforeDoctypeName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                in ASCII_ALPHA_UPPER -> {
                    requireNotNull(ch)

                    currentToken = Token.Doctype(name = ch.asciiLowercase().toString())
                    switchState(DoctypeName)
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentToken = Token.Doctype(name = REPLACEMENT_CHAR.toString())
                    switchState(DoctypeName)
                }
                '>' -> {
                    // missing-doctype-name parse error

                    switchState(Data)
                    Token.Doctype(forceQuirks = true)
                }
                null -> {
                    // eof-in-doctype parse error

                    emitMany(
                        Token.Doctype(forceQuirks = true),
                        Token.EndOfFile,
                    )
                }
                else -> {
                    currentToken = Token.Doctype(ch.toString())
                    switchState(DoctypeName)
                }
            }
        }),
        DoctypeName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> switchState(AfterDoctypeName)
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                in ASCII_ALPHA_UPPER -> {
                    requireNotNull(ch)

                    currentTokenAs<Token.Doctype>().name += ch.asciiLowercase()
                    null
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Doctype>().name += REPLACEMENT_CHAR
                    null
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    currentTokenAs<Token.Doctype>().name += ch
                    null
                }
            }
        }),
        AfterDoctypeName({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    val chars = "$ch${input.peekAsString(5)}"
                    when {
                        chars.equals("PUBLIC", ignoreCase = true) -> {
                            repeat(5) { input.next() }
                            switchState(AfterDoctypePublicKeyword)
                        }
                        chars.equals("SYSTEM", ignoreCase = true) -> {
                            repeat(5) { input.next() }
                            switchState(AfterDoctypeSystemKeyword)
                        }
                        else -> {
                            // invalid-character-sequence-after-doctype-name parse error

                            currentTokenAs<Token.Doctype>().forceQuirks = true
                            reconsumeIn(BogusDoctype, ch)
                        }
                    }
                }
            }
        }),
        AfterDoctypePublicKeyword({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> switchState(BeforeDoctypePublicIdentifier)
                '"' -> {
                    // missing-whitespace-after-doctype-public-keyword parse error

                    currentTokenAs<Token.Doctype>().publicIdentifier = ""
                    switchState(DoctypePublicIdentifierDoubleQuoted)
                }
                '\'' -> {
                    // missing-whitespace-after-doctype-public-keyword parse error

                    currentTokenAs<Token.Doctype>().publicIdentifier = ""
                    switchState(DoctypePublicIdentifierSingleQuoted)
                }
                '>' -> {
                    // missing-doctype-public-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    emitMany(::currentToken.take()!!)
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    // missing-quote-before-doctype-public-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    reconsumeIn(BogusDoctype, ch)
                }
            }
        }),
        BeforeDoctypePublicIdentifier({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '"' -> {
                    currentTokenAs<Token.Doctype>().publicIdentifier = ""
                    switchState(DoctypePublicIdentifierDoubleQuoted)
                }
                '\'' -> {
                    currentTokenAs<Token.Doctype>().publicIdentifier = ""
                    switchState(DoctypePublicIdentifierSingleQuoted)
                }
                '>' -> {
                    // missing-doctype-public-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    emitMany(::currentToken.take()!!)
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    // missing-quote-before-doctype-public-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    reconsumeIn(BogusDoctype, ch)
                }
            }
        }),
        DoctypePublicIdentifierDoubleQuoted({
            when (val ch = input.nextOrNull()) {
                '"' -> switchState(AfterDoctypePublicIdentifier)
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Doctype>().publicIdentifier += REPLACEMENT_CHAR
                    null
                }
                '>' -> {
                    // abrupt-doctype-public-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    currentTokenAs<Token.Doctype>().publicIdentifier += ch
                    null
                }
            }
        }),
        DoctypePublicIdentifierSingleQuoted({
            when (val ch = input.nextOrNull()) {
                '\'' -> switchState(AfterDoctypePublicIdentifier)
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Doctype>().publicIdentifier += REPLACEMENT_CHAR
                    null
                }
                '>' -> {
                    // abrupt-doctype-public-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    currentTokenAs<Token.Doctype>().publicIdentifier += ch
                    null
                }
            }
        }),
        AfterDoctypePublicIdentifier({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> switchState(BetweenDoctypePublicAndSystemIdentifiers)
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                '"' -> {
                    // missing-whitespace-between-doctype-public-and-system-identifiers parse error

                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierDoubleQuoted)
                }
                '\'' -> {
                    // missing-whitespace-between-doctype-public-and-system-identifiers parse error

                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierSingleQuoted)
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    // missing-quote-before-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    reconsumeIn(BogusDoctype, ch)
                }
            }
        }),
        BetweenDoctypePublicAndSystemIdentifiers({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                '"' -> {
                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierDoubleQuoted)
                }
                '\'' -> {
                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierSingleQuoted)
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    // missing-quote-before-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    reconsumeIn(BogusDoctype, ch)
                }
            }
        }),
        AfterDoctypeSystemKeyword({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> switchState(BeforeDoctypeSystemIdentifier)
                '"' -> {
                    // missing-whitespace-after-doctype-system-keyword parse error

                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierDoubleQuoted)
                }
                '\'' -> {
                    // missing-whitespace-after-doctype-system-keyword parse error

                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierSingleQuoted)
                }
                '>' -> {
                    // missing-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    // missing-quote-before-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    reconsumeIn(BogusDoctype, ch)
                }
            }
        }),
        BeforeDoctypeSystemIdentifier({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '"' -> {
                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierDoubleQuoted)
                }
                '\'' -> {
                    currentTokenAs<Token.Doctype>().systemIdentifier = ""
                    switchState(DoctypeSystemIdentifierSingleQuoted)
                }
                '>' -> {
                    // missing-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    // missing-quote-before-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    reconsumeIn(BogusDoctype, ch)
                }
            }
        }),
        DoctypeSystemIdentifierDoubleQuoted({
            when (val ch = input.nextOrNull()) {
                '"' -> switchState(AfterDoctypeSystemIdentifier)
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Doctype>().systemIdentifier += REPLACEMENT_CHAR
                    null
                }
                '>' -> {
                    // abrupt-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    currentTokenAs<Token.Doctype>().systemIdentifier += ch
                    null
                }
            }
        }),
        DoctypeSystemIdentifierSingleQuoted({
            when (val ch = input.nextOrNull()) {
                '\'' -> switchState(AfterDoctypeSystemIdentifier)
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    currentTokenAs<Token.Doctype>().systemIdentifier += REPLACEMENT_CHAR
                    null
                }
                '>' -> {
                    // abrupt-doctype-system-identifier parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    currentTokenAs<Token.Doctype>().systemIdentifier += ch
                    null
                }
            }
        }),
        AfterDoctypeSystemIdentifier({
            when (val ch = input.nextOrNull()) {
                in WHITESPACES -> null
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                null -> {
                    // eof-in-doctype parse error

                    currentTokenAs<Token.Doctype>().forceQuirks = true
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                }
                else -> {
                    // unexpected-character-after-doctype-system-identifier parse error

                    reconsumeIn(BogusDoctype, ch)
                }
            }
        }),
        BogusDoctype({
            when (val ch = input.nextOrNull()) {
                '>' -> {
                    switchState(Data)
                    ::currentToken.take()
                }
                NULL_CHAR -> {
                    // unexpected-null-character parse error

                    null
                }
                null ->
                    emitMany(
                        ::currentToken.take()!!,
                        Token.EndOfFile,
                    )
                else -> null
            }
        }),
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
    private var currentAttribute: MutablePair<String, String>? = null
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
    private fun switchState(new: State): Nothing? {
        state = new
        return null
    }
    private fun reconsumeIn(state: State, char: Char?) =
        switchState(state).also {
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

/**
 * Following characters are considered as a whitespace:
 * - U+0009 CHARACTER TABULATION (tab)
 * - U+000A LINE FEED (LF)
 * - U+000C FORM FEED (FF)
 * - U+0020 SPACE
 */
private val WHITESPACES = listOf('\t', '\n', '\u000C', ' ')
