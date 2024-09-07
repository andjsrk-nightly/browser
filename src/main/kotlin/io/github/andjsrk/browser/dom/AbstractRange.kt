package io.github.andjsrk.browser.dom

typealias BoundaryPoint = Pair<Node, Int>

interface AbstractRange {
    var start: BoundaryPoint
    var end: BoundaryPoint

    val isCollapsed get() = start == end
}
