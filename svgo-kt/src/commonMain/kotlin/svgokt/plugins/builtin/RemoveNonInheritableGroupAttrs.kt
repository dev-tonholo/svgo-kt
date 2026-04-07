package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections

/**
 * Removes non-inheritable presentation attributes from `<g>` elements.
 *
 * Presentation attributes on a `<g>` are only meaningful if they are
 * inherited by children. Attributes that are not inherited and are not
 * explicitly allowed on groups (such as `transform` or `opacity`) have no
 * rendering effect and can be safely removed.
 *
 * An attribute is removed when ALL of the following are true:
 * - It is a presentation attribute.
 * - It is NOT inheritable.
 * - It is NOT in the explicit allow-list for group-level non-inheritable attrs.
 */
object RemoveNonInheritableGroupAttrs : Plugin<NoPluginParam> {
    override val name: String = "removeNonInheritableGroupAttrs"
    override val description: String =
        "removes non-inheritable group's presentation attributes"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    if (node.name == "g") {
                        val attrsToRemove = node.attributes.keys.filter { attrName ->
                            attrName in Collections.presentationAttrs &&
                                attrName !in Collections.inheritableAttrs &&
                                attrName !in Collections.presentationNonInheritableGroupAttrs
                        }
                        for (key in attrsToRemove) {
                            node.attributes.remove(key)
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
