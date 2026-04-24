package svgokt.style

import dev.tonholo.kss.parser.ast.css.syntax.node.Selector
import svgokt.xast.parseSelectors

/**
 * Checks whether a CSS selector string references an attribute with the given [name].
 *
 * Parses [selector] with kss and walks every compound selector (including
 * nested pseudo-class arguments such as `:not([fill])`) looking for an
 * attribute selector that matches [name]. When [value] is non-null, also
 * verifies that the attribute selector's matcher would select an element
 * whose attribute value equals [value].
 *
 * @param selector The CSS selector string to inspect.
 * @param name The attribute name to look for.
 * @param value Optional attribute value. When supplied, the attribute
 *   selector must match this value according to its CSS matcher semantics.
 * @return true if the selector contains a matching attribute selector.
 */
fun includesAttrSelector(selector: String, name: String, value: String? = null): Boolean {
    for (item in parseSelectors(selector = selector)) {
        if (item.selectors.any { matchesAttr(selector = it, name = name, value = value) }) {
            return true
        }
    }
    return false
}

private fun matchesAttr(selector: Selector, name: String, value: String?): Boolean = when (selector) {
    is Selector.Attribute -> selector.name == name && matchesAttrValue(selector = selector, value = value)
    is Selector.PseudoClass -> selector.parameters.any {
        matchesAttr(selector = it, name = name, value = value)
    }

    is Selector.PseudoElement -> selector.parameters.any {
        matchesAttr(selector = it, name = name, value = value)
    }

    else -> false
}

private fun matchesAttrValue(selector: Selector.Attribute, value: String?): Boolean {
    if (value == null) return true
    val expected = selector.value ?: return true
    return when (selector.matcher) {
        null, "=" -> expected == value
        "~=" -> expected == value || value in expected.trim().split(Regex(pattern = "\\s+"))
        "|=" -> expected == value || value.startsWith(prefix = "$expected-")
        "^=" -> value.startsWith(prefix = expected)
        "$=" -> value.endsWith(suffix = expected)
        "*=" -> value.contains(other = expected)
        else -> false
    }
}
