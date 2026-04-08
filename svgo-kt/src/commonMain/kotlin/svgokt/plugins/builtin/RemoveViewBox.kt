package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

private val viewBoxElems = setOf("pattern", "svg", "symbol")

/**
 * Remove viewBox attribute when it coincides with a width/height box.
 *
 * @see <a href="https://www.w3.org/TR/SVG11/coords.html#ViewBoxAttribute">SVG viewBox</a>
 */
object RemoveViewBox : Plugin<NoPluginParam> {
    override val name: String = "removeViewBox"
    override val description: String = "removes viewBox attribute when possible"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (
                        node.name in viewBoxElems &&
                        node.attributes.containsKey("viewBox") &&
                        node.attributes.containsKey("width") &&
                        node.attributes.containsKey("height")
                    ) {
                        // Skip nested SVG elements
                        if (node.name == "svg" && parentNode?.type?.name != "ROOT") {
                            return@VisitorNode VisitState.Continue
                        }
                        val numbers = node.attributes["viewBox"]
                            ?.split(Regex("[ ,]+"))
                            ?: return@VisitorNode VisitState.Continue
                        val width = node.attributes["width"]
                            ?.replace(Regex("px$"), "")
                        val height = node.attributes["height"]
                            ?.replace(Regex("px$"), "")
                        if (
                            numbers.size >= 4 &&
                            numbers[0] == "0" &&
                            numbers[1] == "0" &&
                            width == numbers[2] &&
                            height == numbers[3]
                        ) {
                            node.attributes.remove("viewBox")
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
