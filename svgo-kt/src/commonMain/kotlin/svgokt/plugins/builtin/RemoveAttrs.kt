package svgokt.plugins.builtin

import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

private const val DEFAULT_SEPARATOR = ":"
private const val FULL_PATTERN_PARTS = 3

/**
 * Parameters for [RemoveAttrs].
 *
 * @property attrs List of patterns to match for removal.
 *   Format: `element:attribute:value` where each segment is a regex.
 *   A single `*` matches anything. Missing segments default to `.*`.
 * @property elemSeparator Separator between element, attribute, and value segments.
 * @property preserveCurrentColor When true, skip removal of fill/stroke attributes
 *   whose value is `currentColor`.
 */
data class RemoveAttrsParams(
    val attrs: List<String>,
    val elemSeparator: String = DEFAULT_SEPARATOR,
    val preserveCurrentColor: Boolean = false,
) : PluginParams,
    Map<String, Any> by mapOf(
        "attrs" to attrs,
        "elemSeparator" to elemSeparator,
        "preserveCurrentColor" to preserveCurrentColor,
    )

/**
 * Remove attributes matching specified patterns.
 *
 * Pattern format: `element:attribute:value`
 * - Each segment is a regex (wrapped into `^...$`)
 * - A single `*` matches anything
 * - Missing segments default to `.*`
 *
 * Examples:
 * - `"fill"` removes fill attribute from all elements
 * - `"path:fill"` removes fill attribute from path elements
 * - `"path:fill:none"` removes fill attribute from path elements where value is "none"
 * - `"(fill|stroke)"` removes both fill and stroke from all elements
 */
val RemoveAttrs = plugin<NoPluginParam> {
    name = "removeAttrs"
    description = "removes specified attributes"
    fn { _, params, _ ->
        val attrsParam = params["attrs"] ?: return@fn null
        val attrPatterns = when (attrsParam) {
            is List<*> -> attrsParam.filterIsInstance<String>()
            is String -> listOf(attrsParam)
            else -> return@fn null
        }
        if (attrPatterns.isEmpty()) return@fn null

        val elemSeparator = (params["elemSeparator"] as? String) ?: DEFAULT_SEPARATOR
        val preserveCurrentColor = (params["preserveCurrentColor"] as? Boolean) ?: false

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    for (rawPattern in attrPatterns) {
                        val pattern = normalizePattern(
                            pattern = rawPattern,
                            separator = elemSeparator,
                        )
                        val parts = pattern.split(elemSeparator).map { segment ->
                            val adjusted = if (segment == "*") ".*" else segment
                            Regex("^$adjusted$", RegexOption.IGNORE_CASE)
                        }
                        if (parts.size < FULL_PATTERN_PARTS) {
                            continue
                        }
                        if (parts[0].matches(node.name)) {
                            val attrsToRemove = mutableListOf<String>()
                            for ((attrName, attrValue) in node.attributes) {
                                val isCurrentColor = attrValue
                                    .lowercase() == "currentcolor"
                                val shouldPreserve = preserveCurrentColor &&
                                    (attrName == "fill" || attrName == "stroke") &&
                                    isCurrentColor
                                if (
                                    !shouldPreserve &&
                                    parts[1].matches(attrName) &&
                                    parts[2].matches(attrValue)
                                ) {
                                    attrsToRemove.add(attrName)
                                }
                            }
                            attrsToRemove.forEach { node.attributes.remove(it) }
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}

private fun normalizePattern(pattern: String, separator: String): String {
    return when {
        !pattern.contains(separator) -> {
            listOf(".*", pattern, ".*").joinToString(separator)
        }
        pattern.split(separator).size < FULL_PATTERN_PARTS -> {
            listOf(pattern, ".*").joinToString(separator)
        }
        else -> pattern
    }
}
