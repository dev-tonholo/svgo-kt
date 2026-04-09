package svgokt.plugins.builtin

import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Removes specified attributes from elements matching a simple selector.
 *
 * This is a simplified implementation that supports element-name selectors
 * and `[attr='value']` attribute selectors rather than full CSS selector
 * parsing. For each selector/attributes pair the plugin walks the tree,
 * finds matching elements, and deletes the listed attributes.
 */
object RemoveAttributesBySelector : Plugin<RemoveAttributesBySelector.Params> {

    data class SelectorDef(
        val selector: String,
        val attributes: List<String>,
    )

    data class Params(
        val selector: String? = null,
        val attributes: List<String> = emptyList(),
        val selectors: List<SelectorDef> = emptyList(),
    ) : PluginParams,
        Map<String, Any> by toMap(selector, attributes, selectors) {
        companion object {
            private fun toMap(
                selector: String?,
                attributes: List<String>,
                selectors: List<SelectorDef>,
            ): Map<String, Any> = buildMap {
                selector?.let { put("selector", it) }
                if (attributes.isNotEmpty()) put("attributes", attributes)
                if (selectors.isNotEmpty()) put("selectors", selectors)
            }
        }
    }

    override val name: String = "removeAttributesBySelector"
    override val description: String = "removes attributes of elements that match a selector"
    override val params: Params = Params()

    private val ATTR_SELECTOR_REGEX = Regex("""\[(\w[\w-]*)=['"](.*?)['"]]""")

    override val fn: PluginFn = fn@{ _, pluginParams, _ ->
        val resolved = resolveParams(pluginParams)
        val defs = resolved.selectors.ifEmpty {
            val sel = resolved.selector ?: return@fn null
            listOf(SelectorDef(selector = sel, attributes = resolved.attributes))
        }
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    for (def in defs) {
                        if (matchesSelector(
                                elementName = node.name,
                                attributes = node.attributes,
                                selector = def.selector,
                            )
                        ) {
                            for (attr in def.attributes) {
                                node.attributes.remove(attr)
                            }
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }

    /**
     * Simplified selector matching supporting:
     * - element name: `rect`
     * - id selector: `#myId`
     * - attribute value: `[fill='#00ff00']`
     * - combinations: `rect[fill='#00ff00']`
     */
    private fun matchesSelector(
        elementName: String,
        attributes: Map<String, String>,
        selector: String,
    ): Boolean {
        val trimmed = selector.trim()

        // Handle #id selector
        if (trimmed.startsWith("#")) {
            val idValue = trimmed.substring(startIndex = 1)
            return attributes["id"] == idValue
        }

        val bracketIndex = trimmed.indexOf('[')
        val elemPart = when {
            bracketIndex > 0 -> trimmed.substring(startIndex = 0, endIndex = bracketIndex)
            bracketIndex < 0 -> trimmed
            else -> null
        }

        val elementMatches = elemPart == null || elemPart.isEmpty() || elemPart == elementName
        if (!elementMatches) return false

        val attrPart = if (bracketIndex >= 0) trimmed.substring(startIndex = bracketIndex) else ""
        return attrPart.isEmpty() || ATTR_SELECTOR_REGEX.findAll(attrPart).all { match ->
            attributes[match.groupValues[1]] == match.groupValues[2]
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveParams(pluginParams: PluginParams): Params {
        if (pluginParams is Params) return pluginParams
        val attributes = when (val raw = pluginParams["attributes"]) {
            is String -> listOf(raw)
            is List<*> -> (raw as List<Any>).map { it.toString() }
            else -> emptyList()
        }
        val selectors = when (val raw = pluginParams["selectors"]) {
            is List<*> -> (raw as List<Any>).mapNotNull { item ->
                when (item) {
                    is SelectorDef -> item
                    is Map<*, *> -> {
                        val sel = item["selector"] as? String ?: return@mapNotNull null
                        val attrs = when (val a = item["attributes"]) {
                            is String -> listOf(a)
                            is List<*> -> (a as List<Any>).map { it.toString() }
                            else -> emptyList()
                        }
                        SelectorDef(selector = sel, attributes = attrs)
                    }
                    else -> null
                }
            }
            else -> emptyList()
        }
        return Params(
            selector = pluginParams["selector"] as? String,
            attributes = attributes,
            selectors = selectors,
        )
    }
}
