package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Collapses useless group elements that add no value to the SVG structure.
 *
 * Two cases are handled:
 *
 * **Case 1 - Bare group (no attributes):** A `<g>` with no attributes is replaced
 * by its children directly in the parent's children list.
 *
 * **Case 2 - Single child group:** A `<g>` whose only child is another `<g>` has
 * its attributes merged into the child `<g>`, then is replaced by that child.
 * Merge rules:
 * - `transform`: values are concatenated (outer + " " + inner)
 * - `class`: values are concatenated with a space
 * - All other attributes: the inner group's value wins (no overwrite)
 *
 * Uses post-order (exit) traversal so inner groups are processed before outer ones,
 * enabling nested collapse in a single pass.
 */
object CollapseGroups : Plugin<NoPluginParam> {
    override val name: String = "collapseGroups"
    override val description: String = "collapses useless groups"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onExit = CollapseGroups::onExit,
            ),
        )
    }

    @Suppress("ReturnCount")
    private fun onExit(node: XastElement, parentNode: XastParent?) {
        if (node.name != "g") return
        if (parentNode == null || parentNode is XastRoot) return
        val parentElement = parentNode as? XastElement ?: return
        if (parentElement.name == "switch") return

        val nodeIndex = parentElement.children.indexOf(node)
        if (nodeIndex < 0) return

        // Case 1: bare g with no attributes - promote children directly
        if (node.attributes.isEmpty()) {
            parentElement.children.removeAt(nodeIndex)
            parentElement.children.addAll(index = nodeIndex, elements = node.children)
            return
        }

        // Case 2: g with exactly one child that is also a g - merge attributes
        val singleChild = node.children.singleOrNull() as? XastElement ?: return
        if (singleChild.name != "g") return

        mergeAttributes(outer = node, inner = singleChild)

        parentElement.children[nodeIndex] = singleChild
    }

    private fun mergeAttributes(outer: XastElement, inner: XastElement) {
        for ((key, outerValue) in outer.attributes) {
            when (key) {
                "transform" -> {
                    val innerValue = inner.attributes[key]
                    inner.attributes[key] = if (innerValue != null) {
                        "$outerValue $innerValue"
                    } else {
                        outerValue
                    }
                }
                "class" -> {
                    val innerValue = inner.attributes[key]
                    inner.attributes[key] = if (innerValue != null) {
                        "$outerValue $innerValue"
                    } else {
                        outerValue
                    }
                }
                else -> {
                    // Inner group value wins - do not overwrite existing attributes
                    if (!inner.attributes.containsKey(key)) {
                        inner.attributes[key] = outerValue
                    }
                }
            }
        }
    }
}
