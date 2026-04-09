package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.SvgElements
import svgokt.plugins.xast.collectStylesheet
import svgokt.style.includesAttrSelector

private const val PARAM_UNKNOWN_CONTENT = "unknownContent"
private const val PARAM_UNKNOWN_ATTRS = "unknownAttrs"
private const val PARAM_DEFAULT_ATTRS = "defaultAttrs"
private const val PARAM_DEFAULT_MARKUP_DECLARATIONS = "defaultMarkupDeclarations"
private const val PARAM_USELESS_OVERRIDES = "uselessOverrides"
private const val PARAM_KEEP_DATA_ATTRS = "keepDataAttrs"
private const val PARAM_KEEP_ARIA_ATTRS = "keepAriaAttrs"
private const val PARAM_KEEP_ROLE_ATTR = "keepRoleAttr"

/**
 * Remove unknown elements content and attributes,
 * remove attributes with default values.
 *
 * Ported from svgo's removeUnknownsAndDefaults.js plugin.
 */
private val standaloneRegex = Regex("""\s*standalone\s*=\s*(["'])no\1""")

object RemoveUnknownsAndDefaults : Plugin<PluginParams> {
    override val name: String = "removeUnknownsAndDefaults"
    override val description: String =
        "removes unknown elements content and attributes, removes attrs with default values"
    override val params: PluginParams = RemoveUnknownsAndDefaultsParams()
    override val fn: PluginFn = fn@{ root, params, _ ->
        val unknownContent = params.boolParam(PARAM_UNKNOWN_CONTENT, defaultValue = true)
        val unknownAttrs = params.boolParam(PARAM_UNKNOWN_ATTRS, defaultValue = true)
        val defaultAttrs = params.boolParam(PARAM_DEFAULT_ATTRS, defaultValue = true)
        val defaultMarkupDeclarations = params.boolParam(
            PARAM_DEFAULT_MARKUP_DECLARATIONS,
            defaultValue = true,
        )
        @Suppress("UNUSED_VARIABLE")
        val uselessOverrides = params.boolParam(PARAM_USELESS_OVERRIDES, defaultValue = true)
        val keepDataAttrs = params.boolParam(PARAM_KEEP_DATA_ATTRS, defaultValue = true)
        val keepAriaAttrs = params.boolParam(PARAM_KEEP_ARIA_ATTRS, defaultValue = true)
        val keepRoleAttr = params.boolParam(PARAM_KEEP_ROLE_ATTR, defaultValue = false)

        val stylesheet = collectStylesheet(root)

        Visitor(
            instruction = if (defaultMarkupDeclarations) {
                VisitorNode(
                    onEnter = { node, _ ->
                        node.value = node.value.replace(regex = standaloneRegex, replacement = "")
                        VisitState.Continue
                    },
                )
            } else {
                null
            },
            element = VisitorNode(
                onEnter = onEnter@{ node, parentNode ->
                    // skip namespaced elements
                    if (node.name.contains(":")) {
                        return@onEnter VisitState.Continue
                    }
                    // skip visiting foreignObject subtree
                    if (node.name == "foreignObject") {
                        return@onEnter VisitState.Skip
                    }

                    // remove unknown element's content
                    if (unknownContent && parentNode is XastElement) {
                        if (!isAllowedChild(parentNode, node)) {
                            parentNode.children.remove(node)
                            return@onEnter VisitState.Continue
                        }
                    }

                    val allowedAttributes = SvgElements.allowedAttributesPerElement[node.name]
                    val attributesDefaults = SvgElements.attributesDefaultsPerElement[node.name]

                    // Compute a simplified parent style map: for presentation
                    // attributes set directly on the parent element, treat them
                    // as static inherited values. This approximates the JS
                    // computeStyle behavior for the most common case.
                    val parentPresentationAttrs: Map<String, String>? =
                        if (parentNode is XastElement) {
                            parentNode.attributes.filterKeys { key ->
                                Collections.presentationAttrs.contains(key)
                            }
                        } else {
                            null
                        }

                    // remove element's unknown attrs and attrs with default values
                    val attrsToRemove = mutableListOf<String>()
                    for ((attrName, attrValue) in node.attributes) {
                        if (shouldSkipAttr(attrName, keepDataAttrs, keepAriaAttrs, keepRoleAttr)) {
                            continue
                        }

                        if (unknownAttrs && allowedAttributes != null &&
                            !allowedAttributes.contains(attrName)
                        ) {
                            attrsToRemove.add(attrName)
                            continue
                        }
                        if (defaultAttrs &&
                            node.attributes["id"] == null &&
                            attributesDefaults != null &&
                            attributesDefaults[attrName] == attrValue
                        ) {
                            // keep defaults if parent has own or inherited style
                            val parentHasStyle = parentPresentationAttrs?.containsKey(attrName) == true
                            val referencedInStylesheet = stylesheet.rules.any { rule ->
                                includesAttrSelector(rule.selector, attrName)
                            }
                            if (!parentHasStyle && !referencedInStylesheet) {
                                attrsToRemove.add(attrName)
                            }
                        }
                        if (uselessOverrides && node.attributes["id"] == null) {
                            val parentValue = parentPresentationAttrs?.get(attrName)
                            if (!Collections.presentationNonInheritableGroupAttrs.contains(attrName) &&
                                parentValue != null &&
                                parentValue == attrValue
                            ) {
                                attrsToRemove.add(attrName)
                            }
                        }
                    }
                    for (key in attrsToRemove) {
                        node.attributes.remove(key)
                    }

                    VisitState.Continue
                },
            ),
        )
    }

    /**
     * Checks whether [child] is allowed as a child of [parent] based on
     * the SVG spec element definitions.
     */
    private fun isAllowedChild(parent: XastElement, child: XastElement): Boolean {
        val allowedChildren = SvgElements.allowedChildrenPerElement[parent.name]
        return if (allowedChildren == null || allowedChildren.isEmpty()) {
            // parent has no known children spec - remove unknown elements
            SvgElements.allowedChildrenPerElement[child.name] != null
        } else {
            // parent has a known children list
            allowedChildren.contains(child.name)
        }
    }

    /**
     * Returns true if the attribute should be skipped (not considered for removal).
     */
    private fun shouldSkipAttr(
        attrName: String,
        keepDataAttrs: Boolean,
        keepAriaAttrs: Boolean,
        keepRoleAttr: Boolean,
    ): Boolean {
        if (keepDataAttrs && attrName.startsWith("data-")) return true
        if (keepAriaAttrs && attrName.startsWith("aria-")) return true
        if (keepRoleAttr && attrName == "role") return true
        // skip xmlns attribute
        if (attrName == "xmlns") return true
        // skip namespaced attributes except xml:* and xlink:*
        if (attrName.contains(":")) {
            val prefix = attrName.substringBefore(":")
            if (prefix != "xml" && prefix != "xlink") return true
        }
        return false
    }
}

/**
 * Default parameters for removeUnknownsAndDefaults.
 */
private class RemoveUnknownsAndDefaultsParams : PluginParams, Map<String, Any> by mapOf(
    PARAM_UNKNOWN_CONTENT to true,
    PARAM_UNKNOWN_ATTRS to true,
    PARAM_DEFAULT_ATTRS to true,
    PARAM_DEFAULT_MARKUP_DECLARATIONS to true,
    PARAM_USELESS_OVERRIDES to true,
    PARAM_KEEP_DATA_ATTRS to true,
    PARAM_KEEP_ARIA_ATTRS to true,
    PARAM_KEEP_ROLE_ATTR to false,
)

private fun PluginParams.boolParam(key: String, defaultValue: Boolean): Boolean {
    val value = this[key] ?: return defaultValue
    return when (value) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull() ?: defaultValue
        else -> defaultValue
    }
}
