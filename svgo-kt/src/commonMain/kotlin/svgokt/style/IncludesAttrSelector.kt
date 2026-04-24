package svgokt.style

import dev.tonholo.kss.parser.ast.css.syntax.node.Selector
import svgokt.xast.parseSelectors

/**
 * Checks whether a CSS selector string references an attribute with the given [name].
 *
 * Parses [selector] with kss and walks every compound selector (including
 * nested pseudo-class arguments such as `:not([fill])`) looking for an
 * attribute selector that matches [name].
 *
 * @param selector The CSS selector string to inspect.
 * @param name The attribute name to look for.
 * @return true if the selector contains an attribute selector referencing [name].
 */
fun includesAttrSelector(selector: String, name: String): Boolean {
    for (item in parseSelectors(selector = selector)) {
        if (item.selectors.any { containsAttr(selector = it, name = name) }) {
            return true
        }
    }
    return false
}

private fun containsAttr(selector: Selector, name: String): Boolean = when (selector) {
    is Selector.Attribute -> selector.name == name
    is Selector.PseudoClass -> selector.parameters.any { containsAttr(selector = it, name = name) }
    is Selector.PseudoElement -> selector.parameters.any { containsAttr(selector = it, name = name) }
    else -> false
}
