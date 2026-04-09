package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.DeprecatedAttrs
import svgokt.plugins.SvgElements
import svgokt.plugins.attrsGroupsDeprecated
import svgokt.plugins.xast.collectStylesheet
import svgokt.style.includesAttrSelector

private const val PARAM_REMOVE_UNSAFE = "removeUnsafe"

/**
 * Remove deprecated SVG attributes.
 *
 * Ported from svgo's removeDeprecatedAttrs.js plugin.
 * Removes safe deprecated attributes by default. Unsafe attributes
 * are only removed when `removeUnsafe` is set to true.
 */
object RemoveDeprecatedAttrs : Plugin<PluginParams> {
    override val name: String = "removeDeprecatedAttrs"
    override val description: String = "removes deprecated attributes"
    override val params: PluginParams = RemoveDeprecatedAttrsParams()
    override val fn: PluginFn = fn@{ root, params, _ ->
        val removeUnsafe = params.boolParam(PARAM_REMOVE_UNSAFE, defaultValue = false)
        val stylesheet = collectStylesheet(root)
        val attributesInStylesheet = extractAttributesInStylesheet(stylesheet)

        Visitor(
            element = VisitorNode(
                onEnter = onEnter@{ node, _ ->
                    val elemConfig = SvgElements.elems[node.name] ?: return@onEnter VisitState.Continue

                    // Special case: remove xml:lang when lang is also present
                    if (elemConfig.attrsGroups.contains("core") &&
                        node.attributes.containsKey("xml:lang") &&
                        !attributesInStylesheet.contains("xml:lang") &&
                        node.attributes.containsKey("lang")
                    ) {
                        node.attributes.remove("xml:lang")
                    }

                    // General cases: process deprecated attrs from attribute groups
                    for (attrsGroup in elemConfig.attrsGroups) {
                        val deprecated = attrsGroupsDeprecated[attrsGroup]
                        if (deprecated != null) {
                            processAttributes(
                                node = node,
                                deprecatedAttrs = deprecated,
                                removeUnsafe = removeUnsafe,
                                attributesInStylesheet = attributesInStylesheet,
                            )
                        }
                    }

                    // Process element-specific deprecated attrs
                    val elemDeprecated = elemConfig.deprecated
                    if (elemDeprecated != null) {
                        processAttributes(
                            node = node,
                            deprecatedAttrs = elemDeprecated,
                            removeUnsafe = removeUnsafe,
                            attributesInStylesheet = attributesInStylesheet,
                        )
                    }

                    VisitState.Continue
                },
            ),
        )
    }

    private fun processAttributes(
        node: XastElement,
        deprecatedAttrs: DeprecatedAttrs,
        removeUnsafe: Boolean,
        attributesInStylesheet: Set<String>,
    ) {
        for (name in deprecatedAttrs.safe) {
            if (attributesInStylesheet.contains(name)) continue
            node.attributes.remove(name)
        }

        if (removeUnsafe) {
            for (name in deprecatedAttrs.unsafe) {
                if (attributesInStylesheet.contains(name)) continue
                node.attributes.remove(name)
            }
        }
    }

    /**
     * Regex matching CSS attribute selectors, e.g. `[version]`, `[version="1.1"]`.
     * Captures the attribute name in group 1.
     */
    private val attrSelectorRegex = Regex("""\[([a-zA-Z_][\w-]*)(?:[~|^$*]?=)?""")

    /**
     * Extracts attribute names referenced in CSS attribute selectors within
     * the stylesheet. Uses regex instead of a full CSS selector parser to
     * avoid dependency on kss parser internals.
     */
    private fun extractAttributesInStylesheet(
        stylesheet: svgokt.domain.css.Stylesheet,
    ): Set<String> {
        val result = mutableSetOf<String>()
        for (rule in stylesheet.rules) {
            for (match in attrSelectorRegex.findAll(rule.selector)) {
                val attrName = match.groupValues[1]
                if (attrName.isNotEmpty()) {
                    result.add(attrName)
                }
            }
        }
        return result
    }
}

/**
 * Default parameters for removeDeprecatedAttrs.
 */
private class RemoveDeprecatedAttrsParams : PluginParams, Map<String, Any> by mapOf(
    PARAM_REMOVE_UNSAFE to false,
)

private fun PluginParams.boolParam(key: String, defaultValue: Boolean): Boolean {
    val value = this[key] ?: return defaultValue
    return when (value) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull() ?: defaultValue
        else -> defaultValue
    }
}
