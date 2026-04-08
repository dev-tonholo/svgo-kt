package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent

/**
 * Converts single-stop gradients to a plain color.
 *
 * When a `<linearGradient>` or `<radialGradient>` has exactly one `<stop>`
 * child, all references to that gradient (via `url(#id)`) in color
 * attributes are replaced with the stop's `stop-color` value, and the
 * gradient element is removed.
 */
object ConvertOneStopGradients : Plugin<NoPluginParam> {
    override val name: String = "convertOneStopGradients"
    override val description: String = "converts one-stop (single color) gradients to a plain color"
    override val params: NoPluginParam = NoPluginParam

    private const val DEFAULT_STOP_COLOR = "#000"

    private data class GradientInfo(
        val node: XastElement,
        val parent: XastParent,
        val stopColor: String,
    )

    override val fn: PluginFn = { _, _, _ ->
        val gradients = mutableListOf<GradientInfo>()
        val allElements = mutableListOf<XastElement>()

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    allElements.add(node)

                    if (node.name != "linearGradient" && node.name != "radialGradient") {
                        return@VisitorNode VisitState.Continue
                    }

                    val stops = node.children.filterIsInstance<XastElement>()
                        .filter { it.name == "stop" }

                    if (stops.size != 1) {
                        return@VisitorNode VisitState.Continue
                    }

                    val stop = stops.first()
                    val color = stop.attributes["stop-color"] ?: DEFAULT_STOP_COLOR

                    if (parentNode != null) {
                        gradients.add(
                            GradientInfo(
                                node = node,
                                parent = parentNode,
                                stopColor = color,
                            )
                        )
                    }

                    VisitState.Continue
                },
                onExit = { node, _ ->
                    if (node.name != "svg") return@VisitorNode

                    for (gradient in gradients) {
                        val id = gradient.node.attributes["id"] ?: continue
                        val selectorVal = "url(#$id)"

                        // Replace references in color attributes
                        for (element in allElements) {
                            for (attr in Collections.colorsProps) {
                                if (element.attributes[attr] == selectorVal) {
                                    element.attributes[attr] = gradient.stopColor
                                }
                            }

                            // Also handle inline style references
                            val style = element.attributes["style"]
                            if (style != null && style.contains(selectorVal)) {
                                element.attributes["style"] = style.replace(
                                    selectorVal,
                                    gradient.stopColor,
                                )
                            }
                        }

                        // Remove the gradient
                        gradient.node.detachFromParent(gradient.parent)
                    }

                    // Clean up empty defs
                    for (element in allElements) {
                        if (element.name == "defs" && element.children.isEmpty()) {
                            val parent = allElements.find { it.children.contains(element) }
                            if (parent != null) {
                                element.detachFromParent(parent)
                            }
                        }
                    }
                },
            ),
        )
    }
}
