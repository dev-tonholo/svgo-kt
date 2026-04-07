package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

object RemoveMetadata : Plugin<NoPluginParam> {
    override val name: String = "removeMetadata"
    override val description: String = "removes <metadata>"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "metadata") {
                        parentNode?.let { node.detachFromParent(it) }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
