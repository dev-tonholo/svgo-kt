package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent
import svgokt.xast.querySelector

/**
 * Converts single-stop gradients to a plain color.
 *
 * When a `<linearGradient>` or `<radialGradient>` has exactly one `<stop>`
 * child, all references to that gradient (via `url(#id)`) in color
 * attributes are replaced with the stop's `stop-color` value, and the
 * gradient element is removed.
 *
 * Handles gradients that reference other gradients via href/xlink:href.
 */
object ConvertOneStopGradients : Plugin<NoPluginParam> {
    override val name: String = "convertOneStopGradients"
    override val description: String = "converts one-stop (single color) gradients to a plain color"
    override val params: NoPluginParam = NoPluginParam

    private const val DEFAULT_STOP_COLOR = "#000"

    override val fn: PluginFn = { root, _, _ ->
        val allDefs = mutableListOf<Pair<XastElement, XastParent>>()
        val gradientsToDetach = mutableMapOf<XastElement, XastParent>()
        var xlinkHrefCount = 0

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.attributes["xlink:href"] != null) {
                        xlinkHrefCount++
                    }

                    if (node.name == "defs" && parentNode != null) {
                        allDefs.add(node to parentNode)
                        return@VisitorNode VisitState.Continue
                    }

                    if (node.name != "linearGradient" && node.name != "radialGradient") {
                        return@VisitorNode VisitState.Continue
                    }

                    val stops = node.children.filterIsInstance<XastElement>()
                        .filter { it.name == "stop" }

                    // Follow href to referenced gradient when this one has no stops
                    val href = node.attributes["xlink:href"] ?: node.attributes["href"]
                    val effectiveNode = if (stops.isEmpty() && href != null && href.startsWith("#")) {
                        resolveHrefTarget(root, href)
                    } else {
                        node
                    }

                    if (effectiveNode == null) {
                        if (parentNode != null) {
                            gradientsToDetach[node] = parentNode
                        }
                        return@VisitorNode VisitState.Continue
                    }

                    val effectiveStops = effectiveNode.children.filterIsInstance<XastElement>()
                        .filter { it.name == "stop" }

                    if (effectiveStops.size != 1) {
                        return@VisitorNode VisitState.Continue
                    }

                    if (parentNode != null) {
                        gradientsToDetach[node] = parentNode
                    }

                    val color = resolveStopColor(effectiveStops.first())

                    val id = node.attributes["id"]
                    if (id != null) {
                        val selectorVal = "url(#$id)"
                        replaceColorReferences(root, selectorVal, color)
                    }

                    VisitState.Continue
                },
                onExit = { node, _ ->
                    if (node.name != "svg") return@VisitorNode

                    for ((gradient, parent) in gradientsToDetach) {
                        if (gradient.attributes["xlink:href"] != null) {
                            xlinkHrefCount--
                        }
                        gradient.detachFromParent(parent)
                    }

                    if (xlinkHrefCount == 0) {
                        node.attributes.remove("xmlns:xlink")
                    }

                    for ((defs, parent) in allDefs) {
                        if (defs.children.isEmpty()) {
                            defs.detachFromParent(parent)
                        }
                    }
                },
            ),
        )
    }

    /**
     * Resolves the stop-color from a `<stop>` element.
     * Checks the inline `style` attribute first (style takes precedence),
     * then falls back to the `stop-color` attribute.
     */
    private fun resolveStopColor(stop: XastElement): String? {
        // Check style attribute first (takes precedence per CSS cascade)
        val style = stop.attributes["style"]
        if (style != null) {
            val styleColor = parseStopColorFromStyle(style)
            if (styleColor != null) return styleColor
        }

        // Fall back to stop-color attribute
        return stop.attributes["stop-color"]
    }

    /**
     * Parses `stop-color` value from an inline style string.
     */
    private fun parseStopColorFromStyle(style: String): String? {
        val parts = style.split(";")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("stop-color:")) {
                return trimmed.removePrefix("stop-color:").trim()
            }
        }
        return null
    }

    /**
     * Resolves a href target to a gradient element.
     */
    private fun resolveHrefTarget(root: XastRoot, href: String): XastElement? =
        querySelector(node = root, selector = href)

    /**
     * Replaces all references to [selectorVal] in color attributes and style
     * attributes across the entire document.
     */
    private fun replaceColorReferences(
        root: XastRoot,
        selectorVal: String,
        color: String?,
    ) {
        val elements = querySelectorAllElements(root)
        for (element in elements) {
            for (attr in Collections.colorsProps) {
                if (element.attributes[attr] != selectorVal) continue
                if (color != null) {
                    element.attributes[attr] = color
                } else {
                    element.attributes.remove(attr)
                }
            }

            val style = element.attributes["style"]
            if (style != null && style.contains(selectorVal)) {
                element.attributes["style"] = style.replace(
                    selectorVal,
                    color ?: DEFAULT_STOP_COLOR,
                )
            }
        }
    }

    /**
     * Collects all XastElement descendants from the tree.
     */
    private fun querySelectorAllElements(parent: XastParent): List<XastElement> = buildList {
        for (child in parent.children) {
            if (child is XastElement) {
                add(child)
                addAll(querySelectorAllElements(child))
            }
        }
    }
}
