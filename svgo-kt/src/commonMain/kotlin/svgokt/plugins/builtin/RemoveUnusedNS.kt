package svgokt.plugins.builtin

import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Remove unused namespace declarations from the SVG root element.
 *
 * Collects all xmlns:prefix declarations from the SVG root, then tracks
 * which prefixes are actually used in element names or attribute names.
 * Any remaining unused prefixes are removed on exit.
 */
val RemoveUnusedNS = plugin<NoPluginParam> {
    name = "removeUnusedNS"
    description = "removes unused namespaces declaration"
    fn { _, _, _ ->
        val unusedNamespaces = mutableSetOf<String>()
        var svgRootVisited = false

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    // Collect xmlns: declarations from the SVG root element
                    if (node.name == "svg" && !svgRootVisited &&
                        (parentNode == null || parentNode is svgokt.domain.XastRoot)
                    ) {
                        svgRootVisited = true
                        for (key in node.attributes.keys) {
                            if (key.startsWith("xmlns:")) {
                                val prefix = key.removePrefix("xmlns:")
                                unusedNamespaces.add(prefix)
                            }
                        }
                    }

                    // Check namespace usage in element names and attribute names
                    if (unusedNamespaces.isNotEmpty()) {
                        if (node.name.contains(':')) {
                            val prefix = node.name.substringBefore(':')
                            unusedNamespaces.remove(prefix)
                        }
                        for (attrKey in node.attributes.keys) {
                            if (attrKey.contains(':')) {
                                val prefix = attrKey.substringBefore(':')
                                unusedNamespaces.remove(prefix)
                            }
                        }
                    }

                    VisitState.Continue
                },
                onExit = { node, parentNode ->
                    if (node.name == "svg" &&
                        (parentNode == null || parentNode is svgokt.domain.XastRoot)
                    ) {
                        for (prefix in unusedNamespaces) {
                            node.attributes.remove("xmlns:$prefix")
                        }
                    }
                },
            ),
        )
    }
}
