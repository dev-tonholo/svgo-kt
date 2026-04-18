package svgokt.plugins.builtin

import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Remove arbitrary SVG elements by `id` or `class` attribute.
 *
 * Ported from svgo's `removeElementsByAttr` plugin.
 *
 * Parameters:
 * - `id`: a single id string or a list of id strings; elements whose `id`
 *   attribute matches any entry are removed.
 * - `class`: a single class name or a list of class names; elements whose
 *   `class` attribute (space-split) contains any entry are removed.
 */
val RemoveElementsByAttr = plugin<NoPluginParam> {
    name = "removeElementsByAttr"
    description = "removes arbitrary elements by ID or className"
    fn { _, params, _ ->
        val ids = params.stringList(key = "id")
        val classes = params.stringList(key = "class")
        if (ids.isEmpty() && classes.isEmpty()) return@fn null

        Visitor(
            element = VisitorNode(
                onEnter = onEnter@{ node, parentNode ->
                    val elementId = node.attributes["id"]
                    if (elementId != null && ids.contains(elementId)) {
                        parentNode?.children?.remove(node)
                        return@onEnter VisitState.Continue
                    }
                    val classAttribute = node.attributes["class"]
                    if (!classAttribute.isNullOrEmpty() && classes.isNotEmpty()) {
                        val classList = classAttribute.split(' ')
                        if (classes.any { it in classList }) {
                            parentNode?.children?.remove(node)
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}

private fun Map<String, Any>.stringList(key: String): List<String> =
    when (val value = this[key]) {
        null -> emptyList()
        is String -> listOf(value)
        is List<*> -> value.filterIsInstance<String>()
        else -> emptyList()
    }
