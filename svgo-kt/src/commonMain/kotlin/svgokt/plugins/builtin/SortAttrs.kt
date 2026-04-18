package svgokt.plugins.builtin

import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

private val defaultOrder = listOf(
    "id", "width", "height", "x", "x1", "x2", "y", "y1", "y2",
    "cx", "cy", "r", "fill", "stroke", "marker", "d", "points",
)

private const val NS_PRIORITY_XMLNS = 3
private const val NS_PRIORITY_XMLNS_PREFIXED = 2
private const val NS_PRIORITY_OTHER_NS = 1
private const val NS_PRIORITY_DEFAULT = 0

/**
 * Sort element attributes for better gzip compression.
 *
 * xmlns attributes come first (xmlns before xmlns:*), followed by
 * attributes in the default priority order, then remaining attributes
 * sorted alphabetically. Derived attributes (e.g. fill-opacity) sort
 * next to their base attribute (fill).
 */
val SortAttrs = plugin<PluginParams> {
    name = "sortAttrs"
    description = "sort element attributes for better compression"
    fn { _, params, _ ->
        @Suppress("UNCHECKED_CAST")
        val order = (params["order"] as? List<String>) ?: defaultOrder
        val xmlnsOrder = (params["xmlnsOrder"] as? String) ?: "front"

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    val comparator = Comparator<Pair<String, String>> { a, b ->
                        compareAttrs(
                            aName = a.first,
                            bName = b.first,
                            order = order,
                            xmlnsOrder = xmlnsOrder,
                        )
                    }
                    val sorted = node.attributes
                        .map { (key, value) -> key to value }
                        .sortedWith(comparator)
                    node.attributes.clear()
                    sorted.forEach { (k, v) -> node.attributes[k] = v }

                    VisitState.Continue
                },
            ),
        )
    }
}

private fun getNsPriority(attrName: String, xmlnsOrder: String): Int {
    if (xmlnsOrder == "front") {
        if (attrName == "xmlns") return NS_PRIORITY_XMLNS
        if (attrName.startsWith("xmlns:")) return NS_PRIORITY_XMLNS_PREFIXED
    }
    if (attrName.contains(':')) return NS_PRIORITY_OTHER_NS
    return NS_PRIORITY_DEFAULT
}

private fun compareAttrs(
    aName: String,
    bName: String,
    order: List<String>,
    xmlnsOrder: String,
): Int {
    // Sort namespaces
    val aPriority = getNsPriority(attrName = aName, xmlnsOrder = xmlnsOrder)
    val bPriority = getNsPriority(attrName = bName, xmlnsOrder = xmlnsOrder)
    val priorityNs = bPriority - aPriority
    if (priorityNs != 0) return priorityNs

    // Extract the first part from attributes (e.g. "fill" from "fill-opacity")
    val aPart = aName.split('-').first()
    val bPart = bName.split('-').first()

    // Rely on alphabetical sort when the first part is the same
    if (aPart != bPart) {
        val aInOrder = order.contains(aPart)
        val bInOrder = order.contains(bPart)

        // Sort by position in order param
        if (aInOrder && bInOrder) {
            return order.indexOf(aPart) - order.indexOf(bPart)
        }

        // Put attributes from order param before others
        val aFlag = if (aInOrder) 1 else 0
        val bFlag = if (bInOrder) 1 else 0
        val priorityOrder = bFlag - aFlag
        if (priorityOrder != 0) return priorityOrder
    }

    // Sort alphabetically
    return if (aName < bName) -1 else 1
}
