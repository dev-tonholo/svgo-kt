package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent

/**
 * Removes editor namespace declarations, elements, and attributes.
 *
 * Editors such as Inkscape, Adobe Illustrator, Figma, and Sketch embed
 * editor-specific namespace prefixes and elements into the SVG. These are
 * not needed for rendering and can be safely removed.
 *
 * On the `<svg>` root: any `xmlns:prefix` attribute whose URI matches a
 * known editor namespace is collected and the attribute is deleted.
 *
 * On all elements: attributes whose name starts with a collected prefix
 * are removed, and elements whose tag name starts with a collected prefix
 * are detached entirely.
 */
object RemoveEditorsNSData : Plugin<NoPluginParam> {
    override val name: String = "removeEditorsNSData"
    override val description: String = "removes editors namespaces, elements and attributes"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        val editorPrefixes = mutableSetOf<String>()

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "svg") {
                        val xmlnsKeysToRemove = node.attributes.keys.filter { key ->
                            key.startsWith("xmlns:") &&
                                node.attributes[key] in Collections.editorNamespaces
                        }
                        for (key in xmlnsKeysToRemove) {
                            val prefix = key.removePrefix("xmlns:")
                            editorPrefixes.add(prefix)
                            node.attributes.remove(key)
                        }
                    }

                    if (editorPrefixes.isNotEmpty()) {
                        if (editorPrefixes.any { prefix -> node.name.startsWith("$prefix:") }) {
                            parentNode?.let { node.detachFromParent(it) }
                            return@VisitorNode VisitState.Skip
                        }

                        val attrKeysToRemove = node.attributes.keys.filter { attrKey ->
                            editorPrefixes.any { prefix -> attrKey.startsWith("$prefix:") }
                        }
                        for (key in attrKeysToRemove) {
                            node.attributes.remove(key)
                        }
                    }

                    VisitState.Continue
                },
            ),
        )
    }
}
