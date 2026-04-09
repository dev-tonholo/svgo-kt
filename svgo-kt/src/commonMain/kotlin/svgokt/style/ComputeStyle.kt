package svgokt.style

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.css.ComputedStyles
import svgokt.domain.css.Stylesheet
import svgokt.plugins.Collections
import svgokt.xast.matches

@Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
fun computeOwnStyle(
    stylesheet: Stylesheet,
    node: XastElement,
): Map<String, ComputedStyles> {
    val computedStyle = mutableMapOf<String, ComputedStyles>()
    val importantStyles = mutableMapOf<String, Boolean>()
    for ((name, value) in node.attributes) {
        if (name in Collections.presentationAttrs) {
            computedStyle[name] = ComputedStyles.StaticStyle(inherited = false, value = value)
            importantStyles[name] = false
        }
    }
    @Suppress("UNCHECKED_CAST")
    val parents = stylesheet.parents as Map<XastChild, XastParent>
    for (rule in stylesheet.rules) {
        val ruleMatches = try {
            matches(node = node, selector = rule.selector, parents = parents)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) { false }
        if (!ruleMatches) continue
        for (declaration in rule.declarations) {
            val existing = computedStyle[declaration.name]
            if (existing is ComputedStyles.DynamicStyle) continue
            if (rule.dynamic) {
                computedStyle[declaration.name] = ComputedStyles.DynamicStyle(inherited = false)
                continue
            }
            if (existing == null || declaration.important || importantStyles[declaration.name] == false) {
                computedStyle[declaration.name] = ComputedStyles.StaticStyle(
                    inherited = false,
                    value = declaration.value,
                )
                importantStyles[declaration.name] = declaration.important
            }
        }
    }
    val styleAttr = node.attributes["style"]
    if (styleAttr != null) {
        for (part in styleAttr.split(';')) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            val colonIdx = trimmed.indexOf(':')
            if (colonIdx <= 0) continue
            val name = trimmed.substring(startIndex = 0, endIndex = colonIdx).trim()
            var value = trimmed.substring(startIndex = colonIdx + 1).trim()
            val important = value.contains("!important")
            if (important) value = value.replace("!important", "").trim()
            val existing = computedStyle[name]
            if (existing is ComputedStyles.DynamicStyle) continue
            if (existing == null || important || importantStyles[name] == false) {
                computedStyle[name] = ComputedStyles.StaticStyle(inherited = false, value = value)
                importantStyles[name] = important
            }
        }
    }
    return computedStyle
}

fun computeStyle(
    stylesheet: Stylesheet,
    node: XastElement,
): Map<String, ComputedStyles> {
    @Suppress("UNCHECKED_CAST")
    val parents = stylesheet.parents as Map<XastChild, XastParent>
    val computedStyles = computeOwnStyle(stylesheet, node).toMutableMap()
    var parent: XastParent? = parents[node]
    while (parent != null && parent is XastElement) {
        val inheritedStyles = computeOwnStyle(stylesheet, parent)
        for ((name, computed) in inheritedStyles) {
            if (name !in computedStyles && name in Collections.inheritableAttrs) {
                val inherited = when (computed) {
                    is ComputedStyles.StaticStyle -> computed.copy(inherited = true)
                    is ComputedStyles.DynamicStyle -> computed.copy(inherited = true)
                }
                computedStyles[name] = inherited
            }
        }
        parent = parents[parent]
    }
    return computedStyles
}
