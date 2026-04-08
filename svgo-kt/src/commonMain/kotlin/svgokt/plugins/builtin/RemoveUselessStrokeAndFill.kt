package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent

/**
 * Removes useless stroke and fill attributes.
 *
 * When stroke is "none" or stroke-related values indicate no visible stroke,
 * all stroke-* attributes are removed. Similarly for fill.
 *
 * Simplified version that reads attributes directly instead of using
 * computeStyle.
 */
object RemoveUselessStrokeAndFill : Plugin<RemoveUselessStrokeAndFill.Params> {
    data class Params(
        val stroke: Boolean = true,
        val fill: Boolean = true,
        val removeNone: Boolean = false,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "stroke" to stroke,
            "fill" to fill,
            "removeNone" to removeNone,
        )

    override val name: String = "removeUselessStrokeAndFill"
    override val description: String = "removes useless stroke and fill attributes"
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
        // Elements with id may be referenced; skip the whole subtree
        if (node.attributes.containsKey("id")) return VisitState.Skip
        if (node.name !in Collections.shapeElements) return VisitState.Continue

        if (params.stroke) {
            removeUselessStroke(node, parentNode)
        }
        if (params.fill) {
            removeUselessFill(node)
        }
        if (params.removeNone) {
            removeNoneElement(node, parentNode)
        }
        return VisitState.Continue
    }

    private fun removeUselessStroke(node: XastElement, parentNode: XastParent?) {
        val stroke = node.attributes["stroke"]
        val strokeOpacity = node.attributes["stroke-opacity"]
        val strokeWidth = node.attributes["stroke-width"]
        val isStrokeNone = stroke == "none"
        val isOpacityZero = strokeOpacity == "0"
        val isWidthZero = strokeWidth == "0"

        if (!isStrokeNone && !isOpacityZero && !isWidthZero && stroke != null) return

        node.attributes.keys.removeAll { it.startsWith("stroke") }

        // Set explicit none to prevent inheriting from parent
        val parentElement = parentNode as? XastElement
        val parentStroke = parentElement?.attributes?.get("stroke")
        if (parentStroke != null && parentStroke != "none") {
            node.attributes["stroke"] = "none"
        }
    }

    private fun removeUselessFill(node: XastElement) {
        val fill = node.attributes["fill"]
        val fillOpacity = node.attributes["fill-opacity"]
        val isFillNone = fill == "none"
        val isOpacityZero = fillOpacity == "0"

        if (!isFillNone && !isOpacityZero) return

        node.attributes.keys.removeAll { it.startsWith("fill-") }

        if (fill == null || fill != "none") {
            node.attributes["fill"] = "none"
        }
    }

    private fun removeNoneElement(node: XastElement, parentNode: XastParent?) {
        val stroke = node.attributes["stroke"]
        val fill = node.attributes["fill"]
        val isStrokeNoneOrAbsent = stroke == null || stroke == "none"
        val isFillNone = fill == "none"

        if (isStrokeNoneOrAbsent && isFillNone && parentNode != null) {
            node.detachFromParent(parentNode)
        }
    }
}
