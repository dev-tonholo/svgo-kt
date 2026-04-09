package svgokt.plugins.builtin

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.collectStylesheet
import svgokt.style.computeStyle

/**
 * Collapses useless group elements that add no value to the SVG structure.
 *
 * Two cases are handled:
 *
 * **Case 1 - Single child group with attributes:** A `<g>` with attributes and
 * exactly one element child has its attributes merged into the child, then is
 * replaced by that child. Merge rules follow the JS reference implementation.
 *
 * **Case 2 - Bare group (no attributes):** A `<g>` with no attributes is replaced
 * by its children directly in the parent's children list, unless children include
 * animation elements.
 *
 * Uses post-order (exit) traversal so inner groups are processed before outer ones,
 * enabling nested collapse in a single pass.
 */
object CollapseGroups : Plugin<NoPluginParam> {
    override val name: String = "collapseGroups"
    override val description: String = "collapses useless groups"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { root, _, _ ->
        val stylesheet = collectStylesheet(root)
        Visitor(
            element = VisitorNode(
                onExit = { node, parentNode ->
                    onExit(node = node, parentNode = parentNode, stylesheet = stylesheet)
                },
            ),
        )
    }

    @Suppress("ReturnCount", "CyclomaticComplexMethod", "NestedBlockDepth", "LongMethod")
    private fun onExit(
        node: XastElement,
        parentNode: XastParent?,
        stylesheet: svgokt.domain.css.Stylesheet,
    ) {
        if (parentNode is XastRoot || parentNode == null) return
        val parentElement = parentNode as? XastElement ?: return
        if (parentElement.name == "switch") return

        // Only process non-empty groups
        if (node.name != "g" || node.children.isEmpty()) return

        // Case 1: move group attributes to the single child element
        if (node.attributes.isNotEmpty() && node.children.size == 1) {
            val firstChild = node.children[0]
            if (firstChild is XastElement && firstChild.attributes["id"] == null) {
                val nodeHasFilter = node.attributes.containsKey("filter") ||
                    hasComputedFilter(stylesheet, node)

                if (!nodeHasFilter && canMergeAttributes(node, firstChild)) {
                    val newChildAttrs = LinkedHashMap(firstChild.attributes)

                    for ((attrName, outerValue) in node.attributes) {
                        // Avoid copying if it conflicts with an animated attribute
                        if (hasAnimatedAttr(node = firstChild, attrName = attrName)) {
                            return
                        }

                        val innerValue = newChildAttrs[attrName]
                        if (innerValue == null) {
                            newChildAttrs[attrName] = outerValue
                        } else if (attrName == "transform") {
                            newChildAttrs[attrName] = "$outerValue $innerValue"
                        } else if (innerValue == "inherit") {
                            newChildAttrs[attrName] = outerValue
                        } else if (
                            !Collections.inheritableAttrs.contains(attrName) &&
                            innerValue != outerValue
                        ) {
                            // Non-inheritable attr with different value - can't merge
                            return
                        }
                        // Inheritable attr with different value: child wins, skip
                    }

                    node.attributes.clear()
                    firstChild.attributes.clear()
                    firstChild.attributes.putAll(newChildAttrs)
                }
            }
        }

        // Case 2: collapse groups without attributes
        if (node.attributes.isEmpty()) {
            // Animation elements "add" attributes to group - group should be preserved
            for (child in node.children) {
                if (child is XastElement && Collections.animationElements.contains(child.name)) {
                    return
                }
            }

            // Replace current node with all its children
            val index = parentElement.children.indexOf(node)
            if (index < 0) return
            parentElement.children.removeAt(index)
            parentElement.children.addAll(index = index, elements = node.children)
        }
    }

    /**
     * Checks whether the group's attributes can be merged with the child's attributes.
     *
     * Conditions (matching JS reference):
     * - class attributes must not collide (only one side can have class)
     * - if the group has clip-path or mask, then the child must be a `<g>` and
     *   neither the group nor the child may have transform
     */
    private fun canMergeAttributes(group: XastElement, child: XastElement): Boolean {
        // class collision check: both sides must not have class
        if (group.attributes.containsKey("class") && child.attributes.containsKey("class")) {
            return false
        }

        // clip-path/mask with transform check
        if (group.attributes.containsKey("clip-path") || group.attributes.containsKey("mask")) {
            if (child.name != "g") return false
            if (group.attributes.containsKey("transform")) return false
            if (child.attributes.containsKey("transform")) return false
        }

        return true
    }

    /**
     * Checks if the computed style for a node includes a filter.
     * Since computeStyle is a stub, we also check the style attribute directly.
     */
    private fun hasComputedFilter(
        stylesheet: svgokt.domain.css.Stylesheet,
        node: XastElement,
    ): Boolean {
        val computed = computeStyle(stylesheet, node)
        // The stub returns DynamicStyle, so also check inline style attribute
        val styleAttr = node.attributes["style"] ?: return false
        return styleAttr.contains("filter")
    }

    /**
     * Recursively checks whether any animation descendant targets the given attribute name.
     */
    private fun hasAnimatedAttr(node: XastChild, attrName: String): Boolean {
        if (node !is XastElement) return false
        if (Collections.animationElements.contains(node.name) &&
            node.attributes["attributeName"] == attrName
        ) {
            return true
        }
        for (child in node.children) {
            if (hasAnimatedAttr(node = child, attrName = attrName)) {
                return true
            }
        }
        return false
    }
}
