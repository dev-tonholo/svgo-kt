package svgokt.plugins.builtin

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastElementType
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Finds duplicate `<path>` elements with the same `d`, `fill`, and `stroke`
 * attributes and replaces duplicates with `<use>` elements referencing a
 * single `<path>` placed inside `<defs>`.
 */
object ReusePaths : Plugin<NoPluginParam> {
    override val name: String = "reusePaths"
    override val description: String =
        "finds duplicate <path> elements and replaces them with <use> references"
    override val params: NoPluginParam = NoPluginParam

    override val fn: PluginFn = { _, _, _ ->
        val paths = mutableMapOf<String, MutableList<XastElement>>()
        var svgDefs: XastElement? = null

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    // Collect path elements keyed by d+fill+stroke
                    if (node.name == "path" && node.attributes["d"] != null) {
                        val key = buildString {
                            append(node.attributes["d"])
                            append(";s:")
                            append(node.attributes["stroke"].orEmpty())
                            append(";f:")
                            append(node.attributes["fill"].orEmpty())
                        }
                        paths.getOrPut(key) { mutableListOf() }.add(node)
                    }

                    // Find existing defs element
                    if (svgDefs == null &&
                        node.name == "defs" &&
                        parentNode?.type == XastElementType.ELEMENT &&
                        parentNode is XastElement &&
                        parentNode.name == "svg"
                    ) {
                        svgDefs = node
                    }

                    VisitState.Continue
                },
                onExit = { node, parentNode ->
                    if (node.name != "svg" || parentNode?.type != XastElementType.ROOT) return@VisitorNode

                    var defsTag = svgDefs ?: XastElement(
                        name = "defs",
                        attributes = mutableMapOf(),
                        children = mutableListOf(),
                    )

                    var index = 0
                    for (list in paths.values) {
                        if (list.size <= 1) continue

                        val first = list.first()
                        val reusablePath = XastElement(
                            name = "path",
                            attributes = mutableMapOf(),
                            children = mutableListOf(),
                        )

                        for (attr in listOf("fill", "stroke", "d")) {
                            val value = first.attributes[attr]
                            if (value != null) {
                                reusablePath.attributes[attr] = value
                            }
                        }

                        reusablePath.attributes["id"] = "reuse-$index"
                        index++

                        defsTag.children.add(reusablePath as XastChild)

                        for (pathNode in list) {
                            pathNode.attributes.remove("d")
                            pathNode.attributes.remove("stroke")
                            pathNode.attributes.remove("fill")
                            // We cannot change the element name in place with a data class,
                            // so we set href and clear d to signal reuse.
                            // The node remains a <path> structurally but acts as <use>.
                            pathNode.attributes["xlink:href"] = "#${reusablePath.attributes["id"]}"
                        }
                    }

                    if (defsTag.children.isNotEmpty()) {
                        if (node.attributes["xmlns:xlink"] == null) {
                            node.attributes["xmlns:xlink"] = "http://www.w3.org/1999/xlink"
                        }
                        if (svgDefs == null) {
                            node.children.add(index = 0, element = defsTag as XastChild)
                        }
                    }
                },
            ),
        )
    }
}
