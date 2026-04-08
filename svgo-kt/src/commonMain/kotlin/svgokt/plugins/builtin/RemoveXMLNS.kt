package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Remove the xmlns attribute when present (for inline SVG).
 *
 * Example:
 * ```
 * <svg viewBox="0 0 100 50" xmlns="http://www.w3.org/2000/svg">
 *   becomes
 * <svg viewBox="0 0 100 50">
 * ```
 */
object RemoveXMLNS : Plugin<NoPluginParam> {
    override val name: String = "removeXMLNS"
    override val description: String = "removes xmlns attribute (for inline svg)"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    if (node.name == "svg") {
                        node.attributes.remove("xmlns")
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
