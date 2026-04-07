package svgokt.plugins.builtin

import svgokt.domain.XastText
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

object RemoveDesc : Plugin<NoPluginParam> {
    private val generatorPattern = Regex("^(Created with|Created using)")

    override val name: String = "removeDesc"
    override val description: String = "removes <desc> if it is not for accessibility purposes"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "desc") {
                        val firstChild = node.children.firstOrNull()
                        val shouldRemove = firstChild == null ||
                            (firstChild is XastText && generatorPattern.containsMatchIn(firstChild.value))
                        if (shouldRemove) {
                            parentNode?.let { node.detachFromParent(it) }
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
