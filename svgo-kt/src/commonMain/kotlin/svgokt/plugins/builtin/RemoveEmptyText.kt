package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

object RemoveEmptyText : Plugin<NoPluginParam> {
    override val name: String = "removeEmptyText"
    override val description: String = "remove empty text-related elements"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = RemoveEmptyText::onEnter,
            ),
        )
    }

    private fun onEnter(node: XastElement, parentNode: XastParent?): VisitState {
        when (node.name) {
            "text", "tspan" -> {
                if (node.children.isEmpty()) {
                    parentNode?.let { node.detachFromParent(it) }
                }
            }
            "tref" -> {
                if (node.attributes["xlink:href"] == null) {
                    parentNode?.let { node.detachFromParent(it) }
                }
            }
        }
        return VisitState.Continue
    }
}
