package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

/**
 * Removes hidden elements that are not rendered:
 * - Elements with display="none"
 * - Elements with visibility="hidden"
 * - Circles with r="0"
 * - Ellipses with rx="0" or ry="0"
 * - Rectangles with width="0" or height="0"
 * - Paths with empty d attribute
 * - Images with width="0" or height="0"
 * - Patterns with width="0" or height="0"
 * - Polylines/polygons with missing points
 *
 * Simplified version without computeStyle; checks attributes directly.
 */
object RemoveHiddenElems : Plugin<RemoveHiddenElems.Params> {
    data class Params(
        val displayNone: Boolean = true,
        val isHidden: Boolean = true,
        val circleR0: Boolean = true,
        val ellipseRX0: Boolean = true,
        val ellipseRY0: Boolean = true,
        val rectWidth0: Boolean = true,
        val rectHeight0: Boolean = true,
        val patternWidth0: Boolean = true,
        val patternHeight0: Boolean = true,
        val imageWidth0: Boolean = true,
        val imageHeight0: Boolean = true,
        val pathEmptyD: Boolean = true,
        val polylineEmptyPoints: Boolean = true,
        val polygonEmptyPoints: Boolean = true,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "displayNone" to displayNone,
            "isHidden" to isHidden,
            "circleR0" to circleR0,
            "ellipseRX0" to ellipseRX0,
            "ellipseRY0" to ellipseRY0,
            "rectWidth0" to rectWidth0,
            "rectHeight0" to rectHeight0,
            "patternWidth0" to patternWidth0,
            "patternHeight0" to patternHeight0,
            "imageWidth0" to imageWidth0,
            "imageHeight0" to imageHeight0,
            "pathEmptyD" to pathEmptyD,
            "polylineEmptyPoints" to polylineEmptyPoints,
            "polygonEmptyPoints" to polygonEmptyPoints,
        )

    override val name: String = "removeHiddenElems"
    override val description: String =
        "removes hidden elements (zero sized, with absent attributes)"
    override val params: Params = Params()
    override val fn: PluginFn = { _, pluginParams, _ ->
        val resolvedParams = pluginParams as? Params ?: Params()
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    onEnter(node, parentNode, resolvedParams)
                },
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): VisitState {
        if (isInsideDefs(parentNode)) return VisitState.Continue
        if (tryRemoveDisplayNone(node, parentNode, params)) return VisitState.Continue
        if (tryRemoveHidden(node, parentNode, params)) return VisitState.Continue
        if (tryRemoveZeroSized(node, parentNode, params)) return VisitState.Continue
        if (tryRemoveEmptyPath(node, parentNode, params)) return VisitState.Continue
        if (tryRemoveEmptyPoints(node, parentNode, params)) return VisitState.Continue
        return VisitState.Continue
    }

    private fun isInsideDefs(parentNode: XastParent?): Boolean {
        val parentElement = parentNode as? XastElement ?: return false
        return parentElement.name == "defs"
    }

    private fun tryRemoveDisplayNone(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): Boolean {
        if (!params.displayNone) return false
        if (node.attributes["display"] != "none") return false
        // markers with display:none are still rendered
        if (node.name == "marker") return false
        parentNode?.let { node.detachFromParent(it) }
        return true
    }

    private fun tryRemoveHidden(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): Boolean {
        if (!params.isHidden) return false
        if (node.attributes["visibility"] != "hidden") return false
        parentNode?.let { node.detachFromParent(it) }
        return true
    }

    private fun tryRemoveZeroSized(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): Boolean {
        if (node.children.isNotEmpty()) return false
        if (!isZeroSizedElement(node, params)) return false
        parentNode?.let { node.detachFromParent(it) }
        return true
    }

    /**
     * Returns the list of zero-size checks for the given element name.
     * Each check is a pair of (param enabled, attribute equals "0").
     */
    private fun zeroSizeChecks(
        node: XastElement,
        params: Params,
    ): List<Pair<Boolean, Boolean>> = when (node.name) {
        "circle" -> listOf(params.circleR0 to (node.attributes["r"] == "0"))
        "ellipse" -> listOf(
            params.ellipseRX0 to (node.attributes["rx"] == "0"),
            params.ellipseRY0 to (node.attributes["ry"] == "0"),
        )
        "rect" -> listOf(
            params.rectWidth0 to (node.attributes["width"] == "0"),
            params.rectHeight0 to (node.attributes["height"] == "0"),
        )
        "pattern" -> listOf(
            params.patternWidth0 to (node.attributes["width"] == "0"),
            params.patternHeight0 to (node.attributes["height"] == "0"),
        )
        "image" -> listOf(
            params.imageWidth0 to (node.attributes["width"] == "0"),
            params.imageHeight0 to (node.attributes["height"] == "0"),
        )
        else -> emptyList()
    }

    private fun isZeroSizedElement(node: XastElement, params: Params): Boolean =
        zeroSizeChecks(node, params).any { (enabled, isZero) -> enabled && isZero }

    private fun tryRemoveEmptyPath(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): Boolean {
        if (!params.pathEmptyD) return false
        if (node.name != "path") return false
        val d = node.attributes["d"]
        if (d != null && d.isNotBlank()) return false
        parentNode?.let { node.detachFromParent(it) }
        return true
    }

    private fun tryRemoveEmptyPoints(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): Boolean {
        val shouldRemove = when (node.name) {
            "polyline" -> params.polylineEmptyPoints && !node.attributes.containsKey("points")
            "polygon" -> params.polygonEmptyPoints && !node.attributes.containsKey("points")
            else -> false
        }
        if (shouldRemove && parentNode != null) {
            node.detachFromParent(parentNode)
        }
        return shouldRemove
    }
}
