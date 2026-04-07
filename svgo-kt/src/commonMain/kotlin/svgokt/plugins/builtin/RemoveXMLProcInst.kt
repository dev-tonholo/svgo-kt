package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

object RemoveXMLProcInst : Plugin<NoPluginParam> {
    override val name: String = "removeXMLProcInst"
    override val description: String = "removes XML processing instructions"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            instruction = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "xml") {
                        parentNode?.let { node.detachFromParent(it) }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
