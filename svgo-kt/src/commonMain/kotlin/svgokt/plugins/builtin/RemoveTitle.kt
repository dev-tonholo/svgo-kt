package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

/**
 * Remove `<title>` elements.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/SVG/Element/title">MDN - title</a>
 */
object RemoveTitle : Plugin<NoPluginParam> {
    override val name: String = "removeTitle"
    override val description: String = "removes <title>"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "title") {
                        parentNode?.let { node.detachFromParent(it) }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
