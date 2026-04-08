package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

/**
 * Remove `<style>` elements.
 *
 * @see <a href="https://www.w3.org/TR/SVG11/styling.html#StyleElement">SVG Style Element</a>
 */
object RemoveStyleElement : Plugin<NoPluginParam> {
    override val name: String = "removeStyleElement"
    override val description: String = "removes <style> element"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "style") {
                        parentNode?.let { node.detachFromParent(it) }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
