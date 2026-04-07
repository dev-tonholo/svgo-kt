package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Moves a group's `transform` attribute down to its child elements when safe.
 *
 * The optimization is applied only when ALL of the following are true:
 * - The group has at least one child element.
 * - The group has a `transform` attribute.
 * - None of the group's attribute values contains a URL reference (e.g. `url(`).
 * - None of the children has an `id` attribute (referencing a child by id would
 *   break if the transform moved to it and changed its coordinate space).
 *
 * When safe, the group's transform is prepended to each child's existing transform
 * and the attribute is removed from the group.
 */
object MoveGroupAttrsToElems : Plugin<NoPluginParam> {
    override val name: String = "moveGroupAttrsToElems"
    override val description: String =
        "moves group's transform attribute to child elements when safe"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = ::onEnter,
            ),
        )
    }

    private fun onEnter(node: XastElement, @Suppress("UNUSED_PARAMETER") parentNode: XastParent?): VisitState {
        if (node.name != "g") return VisitState.Continue

        val children = node.children.filterIsInstance<XastElement>()
        if (children.isEmpty()) return VisitState.Continue

        val transform = node.attributes["transform"] ?: return VisitState.Continue

        val hasUrlReference = node.attributes.values.any { includesUrlReference(it) }
        if (hasUrlReference) return VisitState.Continue

        val childHasId = children.any { it.attributes.containsKey("id") }
        if (childHasId) return VisitState.Continue

        for (child in children) {
            val childTransform = child.attributes["transform"]
            child.attributes["transform"] = if (childTransform != null) {
                "$transform $childTransform"
            } else {
                transform
            }
        }

        node.attributes.remove("transform")
        return VisitState.Continue
    }

    private fun includesUrlReference(value: String): Boolean = value.contains("url(")
}
