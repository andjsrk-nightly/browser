package io.github.andjsrk.browser.dom

/**
 * Indicates that the annotated interface is used for [elementTypes] elements.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class UsedByElements(vararg val elementTypes: HtmlElementType)
