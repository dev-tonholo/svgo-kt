package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.domain.builder.plugins.plugin

private val defaultOrder = listOf(
    "id", "width", "height", "x", "x1", "x2", "y", "y1", "y2",
    "cx", "cy", "r", "fill", "stroke", "marker", "d", "points",
)

/**
 * Sort element attributes for better gzip compression.
 *
 * xmlns attributes come first (xmlns before xmlns:*), followed by
 * attributes in the default priority order, then remaining attributes
 * sorted alphabetically.
 */
val SortAttrs = plugin<NoPluginParam> {
    name = "sortAttrs"
    description = "sort element attributes for better compression"
    fn { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    val comparator = compareBy<Map.Entry<String, String>> { (key, _) ->
                        when {
                            key == "xmlns" -> 0
                            key.startsWith("xmlns:") -> 1
                            else -> 2
                        }
                    }.thenBy { (key, _) ->
                        val orderIndex = defaultOrder.indexOf(key)
                        if (orderIndex >= 0) orderIndex else defaultOrder.size
                    }.thenBy { (key, _) ->
                        val isXmlns = key == "xmlns" || key.startsWith("xmlns:")
                        if (isXmlns) "" else key
                    }

                    val sorted = node.attributes.entries.sortedWith(comparator)
                    node.attributes.clear()
                    sorted.forEach { (k, v) -> node.attributes[k] = v }

                    VisitState.Continue
                },
            ),
        )
    }
}
