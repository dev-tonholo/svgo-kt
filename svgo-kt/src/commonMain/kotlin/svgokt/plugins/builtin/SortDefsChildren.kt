package svgokt.plugins.builtin

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.domain.builder.plugins.plugin

/**
 * Sort children of <defs> elements by:
 * 1. Frequency of element name (most common first)
 * 2. Element name length (longer first)
 * 3. Element name alphabetically (descending)
 */
val SortDefsChildren = plugin<NoPluginParam> {
    name = "sortDefsChildren"
    description = "sort children of <defs> element in order to improve compression"
    fn { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    if (node.name == "defs") {
                        val freq = mutableMapOf<String, Int>()
                        for (child in node.children) {
                            if (child is XastElement) {
                                freq[child.name] = (freq[child.name] ?: 0) + 1
                            }
                        }

                        node.children.sortWith(
                            compareBy<XastChild> { child ->
                                if (child is XastElement) -(freq[child.name] ?: 0) else 0
                            }.thenBy { child ->
                                if (child is XastElement) -child.name.length else 0
                            }.thenByDescending { child ->
                                if (child is XastElement) child.name else ""
                            },
                        )
                    }

                    VisitState.Continue
                },
            ),
        )
    }
}
