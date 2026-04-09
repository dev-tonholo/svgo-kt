package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.visit

/**
 * Path element names, matching the JS `pathElems` set in _collections.js.
 */
private val pathElems = setOf("glyph", "missing-glyph", "path")

/**
 * Moves common inheritable attributes from `<g>` children up to the group element.
 *
 * The optimization is applied in post-order (exit) so all children have been
 * fully processed before the group is evaluated. For each `<g>`:
 * - Skipped when the group has 0 or 1 children.
 * - Skipped when any child is not an XastElement.
 * - Skipped when a `<style>` element exists in the document (selectors may
 *   rely on id, classes, or tag names).
 * - Computes the intersection of inheritable attributes that are present on ALL
 *   children with the same value.
 * - Excludes `transform` from the common set when the group has a `filter`,
 *   `clip-path`, or `mask` attribute (moving transforms in that case would change
 *   the rendering).
 * - Excludes `transform` when all children are path elements (so that the
 *   transform can be applied to path data by other plugins).
 * - For each surviving common attribute:
 *   - If it is `transform`: prepends the group's existing transform (if any) to the
 *     child's value and sets the result on the group; all child transforms are removed.
 *   - Otherwise: sets the value on the group and removes it from every child.
 */
object MoveElemsAttrsToGroup : Plugin<NoPluginParam> {
    override val name: String = "moveElemsAttrsToGroup"
    override val description: String =
        "moves common inheritable attributes from group children up to the group"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { root, _, _ ->
        val deoptimizedWithStyles = hasStyleElement(root)

        Visitor(
            element = VisitorNode(
                onExit = { node, parentNode ->
                    onExit(
                        node = node,
                        parentNode = parentNode,
                        deoptimizedWithStyles = deoptimizedWithStyles,
                    )
                },
            ),
        )
    }

    private fun hasStyleElement(root: XastRoot): Boolean {
        var found = false
        root.visit(
            visitor = Visitor(
                element = VisitorNode(
                    onEnter = { node, _ ->
                        if (node.name == "style") {
                            found = true
                        }
                        VisitState.Continue
                    },
                ),
            ),
        )
        return found
    }

    private fun onExit(
        node: XastElement,
        @Suppress("UNUSED_PARAMETER") parentNode: XastParent?,
        deoptimizedWithStyles: Boolean,
    ) {
        if (node.name != "g") return
        if (node.children.size <= 1) return
        if (deoptimizedWithStyles) return

        val children = node.children.filterIsInstance<XastElement>()
        if (children.size != node.children.size) return

        val commonAttrs = computeCommonInheritableAttrs(children)

        val groupHasFilterOrClipOrMask = node.attributes.containsKey("filter") ||
            node.attributes.containsKey("clip-path") ||
            node.attributes.containsKey("mask")

        if (groupHasFilterOrClipOrMask) {
            commonAttrs.remove("transform")
        }

        // Preserve transform when all children are path elements so that
        // the transform can be applied to path data by other plugins.
        val everyChildIsPath = children.all { child -> child.name in pathElems }
        if (everyChildIsPath) {
            commonAttrs.remove("transform")
        }

        for ((attrName, attrValue) in commonAttrs) {
            if (attrName == "transform") {
                val groupTransform = node.attributes["transform"]
                val newGroupTransform = if (groupTransform != null) {
                    "$groupTransform $attrValue"
                } else {
                    attrValue
                }
                node.attributes["transform"] = newGroupTransform
                for (child in children) {
                    child.attributes.remove("transform")
                }
            } else {
                node.attributes[attrName] = attrValue
                for (child in children) {
                    child.attributes.remove(attrName)
                }
            }
        }
    }

    private fun computeCommonInheritableAttrs(
        children: List<XastElement>,
    ): MutableMap<String, String> {
        val first = children.first()
        val common = first.attributes
            .filter { (name, _) -> name in Collections.inheritableAttrs }
            .toMutableMap()

        for (child in children.drop(n = 1)) {
            val keysToRemove = common.keys.filter { name ->
                child.attributes[name] != common[name]
            }
            for (key in keysToRemove) {
                common.remove(key)
            }
        }

        return common
    }
}
