package io.github.andjsrk.browser.dom

interface Text: CharacterData {
    class Impl(data: String): Text, CharacterData by CharacterData.Impl(data)
}
