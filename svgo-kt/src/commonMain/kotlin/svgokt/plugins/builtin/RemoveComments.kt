package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

object RemoveComments : Plugin<NoPluginParam> {
    override val name: String = "removeComments"
    override val description: String = "removes comments"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            comment = VisitorNode(
                onEnter = { node, parentNode ->
                    parentNode?.let { node.detachFromParent(it) }
                    VisitState.Continue
                },
            ),
        )
    }
}
