package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent

/**
 * Removes content of defs and non-rendering elements that have no id attribute
 * (and thus cannot be referenced). If the container becomes empty after
 * removing useless children, the container itself is removed.
 *
 * Keeps children that:
 * - Have an `id` attribute (can be referenced)
 * - Are `<style>` elements (affect rendering globally)
 */
object RemoveUselessDefs : Plugin<NoPluginParam> {
    override val name: String = "removeUselessDefs"
    override val description: String = "removes elements in <defs> without id"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = ::onEnter,
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
    ): VisitState {
        val isDefsOrNonRendering = node.name == "defs" ||
            (node.name in Collections.nonRenderingElements && !node.attributes.containsKey("id"))

        if (!isDefsOrNonRendering) return VisitState.Continue

        val usefulNodes = mutableListOf<XastElement>()
        collectUsefulNodes(node, usefulNodes)

        if (usefulNodes.isEmpty()) {
            parentNode?.let { node.detachFromParent(it) }
        } else {
            node.children.clear()
            node.children.addAll(usefulNodes)
        }
        return VisitState.Continue
    }

    private fun collectUsefulNodes(
        node: XastElement,
        usefulNodes: MutableList<XastElement>,
    ) {
        for (child in node.children) {
            if (child !is XastElement) continue
            if (child.attributes.containsKey("id") || child.name == "style") {
                usefulNodes.add(child)
            } else {
                collectUsefulNodes(child, usefulNodes)
            }
        }
    }
}
