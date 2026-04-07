package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent

/**
 * Removes empty container elements (container elements with no children).
 *
 * A container element is considered safe to remove when:
 * - It is not the root `<svg>` element
 * - It has no children
 * - It is a known container element
 * - It has no `id` attribute (which could make it a reference target)
 *
 * Uses post-order (exit) traversal so children are processed before parents,
 * allowing nested empty containers to be removed in a single pass.
 */
object RemoveEmptyContainers : Plugin<NoPluginParam> {
    override val name: String = "removeEmptyContainers"
    override val description: String = "removes empty container elements"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onExit = RemoveEmptyContainers::onExit,
            ),
        )
    }

    private fun onExit(node: XastElement, parentNode: XastParent?) {
        if (node.name == "svg") return
        if (node.children.isNotEmpty()) return
        if (node.name !in Collections.containerElements) return
        if (node.attributes.containsKey("id")) return

        parentNode?.let { node.detachFromParent(it) }
    }
}
