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
     * - attribute value: `[fill='#00ff00']`
     * - combinations: `rect[fill='#00ff00']`
     */
    private fun matchesSelector(
        elementName: String,
        attributes: Map<String, String>,
        selector: String,
    ): Boolean {
        var remaining = selector.trim()

        // Extract element name prefix if present
        val bracketIndex = remaining.indexOf('[')
        val elemPart = if (bracketIndex > 0) {
            remaining.substring(startIndex = 0, endIndex = bracketIndex)
        } else if (bracketIndex < 0) {
            remaining
        } else {
            null
        }

        if (elemPart != null && elemPart.isNotEmpty() && elemPart != elementName) {
            return false
        }

        if (bracketIndex >= 0) {
            remaining = remaining.substring(startIndex = bracketIndex)
        } else {
            return elemPart != null
        }

        // Check all attribute selectors
        val matches = ATTR_SELECTOR_REGEX.findAll(remaining)
        for (match in matches) {
            val attrName = match.groupValues[1]
            val attrValue = match.groupValues[2]
            if (attributes[attrName] != attrValue) {
                return false
            }
        }
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveParams(pluginParams: PluginParams): Params {
        if (pluginParams is Params) return pluginParams
        return Params(
            selector = pluginParams["selector"] as? String,
            attributes = (pluginParams["attributes"] as? List<String>).orEmpty(),
            selectors = (pluginParams["selectors"] as? List<SelectorDef>).orEmpty(),
        )
    }
}
