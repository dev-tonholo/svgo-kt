package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

object ConvertEllipseToCircle : Plugin<NoPluginParam> {
    override val name: String = "convertEllipseToCircle"
    override val description: String = "convert non-eccentric ellipses to circles"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = ConvertEllipseToCircle::onEnter,
            ),
        )
    }

    private fun onEnter(node: XastElement, parentNode: XastParent?): VisitState {
        if (node.name != "ellipse") {
            return VisitState.Continue
        }

        val rx = node.attributes["rx"] ?: "0"
        val ry = node.attributes["ry"] ?: "0"

        val canConvert = rx == ry || rx == "auto" || ry == "auto"
        if (!canConvert) {
            return VisitState.Continue
        }

        val radius = when {
            rx == "auto" -> ry
            else -> rx
        }

        val newAttributes = node.attributes.toMutableMap().apply {
            remove("rx")
            remove("ry")
            put("r", radius)
        }

        val circle = XastElement(
            name = "circle",
            attributes = newAttributes,
            children = node.children,
        )

        parentNode?.children?.let { siblings ->
            val index = siblings.indexOf(node)
            if (index >= 0) {
                siblings[index] = circle
            }
        }

        return VisitState.Continue
    }
}
