package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

// Simplified version - removes deprecated SVG attributes without CSS stylesheet awareness.
// Full implementation would check CSS selectors to avoid breaking styles that reference
// these attributes (e.g. [xml:space="preserve"]) before removing them.

private const val ATTR_XML_LANG = "xml:lang"
private const val ATTR_XML_SPACE = "xml:space"
private const val ATTR_LANG = "lang"

// xlink attributes deprecated in SVG2 that carry no semantic weight
private val DEPRECATED_XLINK_ATTRS = setOf(
    "xlink:type",
    "xlink:role",
    "xlink:arcrole",
    "xlink:show",
    "xlink:actuate",
)

object RemoveDeprecatedAttrs : Plugin<NoPluginParam> {
    override val name: String = "removeDeprecatedAttrs"
    override val description: String = "removes deprecated SVG attributes"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = RemoveDeprecatedAttrs::onEnter,
            ),
        )
    }

    private fun onEnter(node: XastElement, @Suppress("UNUSED_PARAMETER") parentNode: XastParent?): VisitState {
        // Remove xml:lang only when lang is also present (xml:lang is redundant in that case)
        if (node.attributes.containsKey(ATTR_XML_LANG) && node.attributes.containsKey(ATTR_LANG)) {
            node.attributes.remove(ATTR_XML_LANG)
        }

        // Remove xml:space - deprecated in SVG2
        node.attributes.remove(ATTR_XML_SPACE)

        // Remove deprecated xlink attributes
        for (attr in DEPRECATED_XLINK_ATTRS) {
            node.attributes.remove(attr)
        }

        return VisitState.Continue
    }
}
