package io.github.andjsrk.browser.common.util

import io.github.andjsrk.browser.common.ASCII_ALPHA_UPPER

inline fun Char.asciiLowercase() =
    if (this in ASCII_ALPHA_UPPER) lowercaseChar()
    else this
