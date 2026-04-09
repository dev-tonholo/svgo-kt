package svgokt.plugins.builtin

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Sort children of defs elements by:
 * 1. Frequency of element name (most common first)
 * 2. Element name length (longer first)
 * 3. Element name alphabetically (descending, i.e. reverse alphabetical)
 *
 * Non-element children (text nodes, whitespace) compare as 0 against
 * anything, preserving their relative positions in a stable sort.
 */
val SortDefsChildren = plugin<NoPluginParam> {
    name = "sortDefsChildren"
    description = "sort children of <defs> element in order to improve compression"
    fn { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    if (node.name == "defs") {
                        val frequencies = mutableMapOf<String, Int>()
                        for (child in node.children) {
                            if (child is XastElement) {
                                frequencies[child.name] = (frequencies[child.name] ?: 0) + 1
                            }
                        }

                        node.children.sortWith(
                            Comparator { a, b ->
                                compareDefsChildren(a, b, frequencies)
                            },
                        )
                    }

                    VisitState.Continue
                },
            ),
        )
    }
}

private fun compareDefsChildren(
    a: XastChild,
    b: XastChild,
    frequencies: Map<String, Int>,
): Int {
    if (a !is XastElement || b !is XastElement) return 0

    val aFrequency = frequencies[a.name]
    val bFrequency = frequencies[b.name]
    if (aFrequency != null && bFrequency != null) {
        val frequencyComparison = bFrequency - aFrequency
        if (frequencyComparison != 0) return frequencyComparison
    }

    val lengthComparison = b.name.length - a.name.length
    if (lengthComparison != 0) return lengthComparison

    if (a.name != b.name) {
        return if (a.name > b.name) -1 else 1
    }

    return 0
}
