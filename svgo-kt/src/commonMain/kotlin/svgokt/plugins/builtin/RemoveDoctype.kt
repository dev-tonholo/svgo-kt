package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

object RemoveDoctype : Plugin<NoPluginParam> {
    override val name: String = "removeDoctype"
    override val description: String = "removes doctype declaration"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            doctype = VisitorNode(
                onEnter = { node, parentNode ->
                    parentNode?.let { node.detachFromParent(it) }
                    VisitState.Continue
                },
            ),
        )
    }
}
