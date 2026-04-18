package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastElementType
import svgokt.domain.XastText
import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.SvgElements

private const val XLINK_NAMESPACE = "http://www.w3.org/1999/xlink"

private val SHOW_TO_TARGET = mapOf(
    "new" to "_blank",
    "replace" to "_self",
)

private val LEGACY_ELEMENTS = setOf(
    "cursor",
    "filter",
    "font-face-uri",
    "glyphRef",
    "tref",
)

/**
 * Remove the XLink namespace and rewrite `xlink:*` references to their SVG 2
 * equivalents. Ported from svgo's `removeXlink` plugin.
 *
 * Parameters:
 * - `includeLegacy`: when `true`, also rewrite and drop xlink attributes on
 *   SVG 1.1 legacy elements (`cursor`, `filter`, `font-face-uri`, `glyphRef`,
 *   `tref`). Default `false` — those elements keep their xlink declarations.
 */
val RemoveXlink = plugin<NoPluginParam> {
    name = "removeXlink"
    description =
        "remove xlink namespace and replaces attributes with the SVG 2 equivalent where applicable"
    fn { _, params, _ ->
        val includeLegacy = (params["includeLegacy"] as? Boolean) ?: false

        val xlinkPrefixes = mutableListOf<String>()
        val overriddenPrefixes = mutableListOf<String>()
        val usedInLegacyElement = mutableListOf<String>()

        Visitor(
            element = VisitorNode(
                onEnter = onEnter@{ node, _ ->
                    collectNamespacePrefixes(
                        node = node,
                        xlinkPrefixes = xlinkPrefixes,
                        overriddenPrefixes = overriddenPrefixes,
                    )

                    if (overriddenPrefixes.any { it in xlinkPrefixes }) {
                        return@onEnter VisitState.Continue
                    }

                    rewriteShowAttributes(node = node, xlinkPrefixes = xlinkPrefixes)
                    rewriteTitleAttributes(node = node, xlinkPrefixes = xlinkPrefixes)
                    rewriteHrefAttributes(
                        node = node,
                        xlinkPrefixes = xlinkPrefixes,
                        usedInLegacyElement = usedInLegacyElement,
                        includeLegacy = includeLegacy,
                    )

                    VisitState.Continue
                },
                onExit = { node, _ ->
                    cleanupOnExit(
                        node = node,
                        xlinkPrefixes = xlinkPrefixes,
                        overriddenPrefixes = overriddenPrefixes,
                        usedInLegacyElement = usedInLegacyElement,
                        includeLegacy = includeLegacy,
                    )
                },
            ),
        )
    }
}

private fun collectNamespacePrefixes(
    node: XastElement,
    xlinkPrefixes: MutableList<String>,
    overriddenPrefixes: MutableList<String>,
) {
    for ((key, value) in node.attributes) {
        if (!key.startsWith("xmlns:")) continue
        val prefix = key.substringAfter(delimiter = ":")
        if (value == XLINK_NAMESPACE) {
            xlinkPrefixes += prefix
            continue
        }
        if (prefix in xlinkPrefixes) {
            overriddenPrefixes += prefix
        }
    }
}

private fun findPrefixedAttributes(
    node: XastElement,
    prefixes: List<String>,
    attribute: String,
): List<String> = prefixes
    .map { prefix -> "$prefix:$attribute" }
    .filter { key -> node.attributes[key] != null }

private fun rewriteShowAttributes(node: XastElement, xlinkPrefixes: List<String>) {
    val showAttrs = findPrefixedAttributes(
        node = node,
        prefixes = xlinkPrefixes,
        attribute = "show",
    )
    var showHandled = node.attributes["target"] != null
    for (index in showAttrs.indices.reversed()) {
        val attribute = showAttrs[index]
        val value = node.attributes[attribute]
        val mapping = value?.let(SHOW_TO_TARGET::get)

        if (showHandled || mapping == null) {
            node.attributes.remove(attribute)
            continue
        }

        val defaultTarget = SvgElements.elems[node.name]?.defaults?.get("target")
        if (mapping != defaultTarget) {
            node.attributes["target"] = mapping
        }

        node.attributes.remove(attribute)
        showHandled = true
    }
}

private fun rewriteTitleAttributes(node: XastElement, xlinkPrefixes: List<String>) {
    val titleAttrs = findPrefixedAttributes(
        node = node,
        prefixes = xlinkPrefixes,
        attribute = "title",
    )
    for (index in titleAttrs.indices.reversed()) {
        val attribute = titleAttrs[index]
        val value = node.attributes[attribute] ?: continue
        val alreadyHasTitle = node.children
            .any { child -> child is XastElement && child.name == "title" }

        if (alreadyHasTitle) {
            node.attributes.remove(attribute)
            continue
        }

        val titleElement = XastElement(
            name = "title",
            attributes = mutableMapOf(),
            children = mutableListOf(XastText(value = value)),
            type = XastElementType.ELEMENT,
        )
        node.children.add(index = 0, element = titleElement)
        node.attributes.remove(attribute)
    }
}

private fun rewriteHrefAttributes(
    node: XastElement,
    xlinkPrefixes: List<String>,
    usedInLegacyElement: MutableList<String>,
    includeLegacy: Boolean,
) {
    val hrefAttrs = findPrefixedAttributes(
        node = node,
        prefixes = xlinkPrefixes,
        attribute = "href",
    )
    if (hrefAttrs.isEmpty()) return

    if (node.name in LEGACY_ELEMENTS && !includeLegacy) {
        for (attribute in hrefAttrs) {
            val prefix = attribute.substringBefore(delimiter = ":")
            usedInLegacyElement += prefix
        }
        return
    }

    for (index in hrefAttrs.indices.reversed()) {
        val attribute = hrefAttrs[index]
        val value = node.attributes[attribute] ?: continue
        if (node.attributes["href"] != null) {
            node.attributes.remove(attribute)
            continue
        }
        node.attributes["href"] = value
        node.attributes.remove(attribute)
    }
}

private fun cleanupOnExit(
    node: XastElement,
    xlinkPrefixes: MutableList<String>,
    overriddenPrefixes: MutableList<String>,
    usedInLegacyElement: List<String>,
    includeLegacy: Boolean,
) {
    val keys = node.attributes.keys.toList()
    for (key in keys) {
        val value = node.attributes[key] ?: continue
        val parts = key.split(":", limit = 2)
        val prefix = parts[0]
        val attributeAfterColon = parts.getOrNull(index = 1)

        if (prefix in xlinkPrefixes &&
            prefix !in overriddenPrefixes &&
            prefix !in usedInLegacyElement &&
            !includeLegacy
        ) {
            node.attributes.remove(key)
            continue
        }

        if (key.startsWith("xmlns:") && attributeAfterColon != null &&
            attributeAfterColon !in usedInLegacyElement
        ) {
            if (value == XLINK_NAMESPACE) {
                xlinkPrefixes.removeAt(xlinkPrefixes.indexOf(attributeAfterColon))
                node.attributes.remove(key)
                continue
            }

            if (prefix in overriddenPrefixes) {
                overriddenPrefixes.removeAt(overriddenPrefixes.indexOf(attributeAfterColon))
            }
        }
    }
}
