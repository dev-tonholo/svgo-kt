@file:Suppress("MatchingDeclarationName")

package svgokt.plugins.builtin

import svgokt.domain.XastElementType
import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Parameters for [AddAttributesToSVGElement].
 *
 * @property attributes A list of attributes to add. Each entry can be either:
 *   - A [String] representing an attribute name (value will be empty)
 *   - A [Map] of attribute name to value pairs
 */
data class AddAttributesToSVGElementParams(
    val attributes: List<Any>,
) : PluginParams,
    Map<String, Any> by mapOf(
        "attributes" to attributes,
    )

/**
 * Add attributes to the outer `<svg>` element.
 *
 * Attributes are only added if they are not already present.
 */
val AddAttributesToSVGElement = plugin<NoPluginParam> {
    name = "addAttributesToSVGElement"
    description = "adds attributes to an outer <svg> element"
    fn { _, params, _ ->
        val attributes = resolveAttributes(params)
            ?: return@fn null

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "svg" && parentNode?.type == XastElementType.ROOT) {
                        for (attribute in attributes) {
                            when (attribute) {
                                is String -> {
                                    if (!node.attributes.containsKey(attribute)) {
                                        node.attributes[attribute] = ""
                                    }
                                }
                                is Map<*, *> -> {
                                    for ((key, value) in attribute) {
                                        val keyStr = key?.toString() ?: continue
                                        if (!node.attributes.containsKey(keyStr)) {
                                            node.attributes[keyStr] = value?.toString() ?: ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}

@Suppress("UNCHECKED_CAST")
private fun resolveAttributes(params: PluginParams): List<Any>? {
    val attributesList = params["attributes"] as? List<Any>
    val singleAttribute = params["attribute"]
    return when {
        attributesList != null -> attributesList
        singleAttribute != null -> listOf(singleAttribute)
        else -> null
    }
}
