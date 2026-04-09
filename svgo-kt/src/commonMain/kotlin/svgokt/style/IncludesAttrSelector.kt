package svgokt.style

import svgokt.xast.parseSelectorListItems

/**
 * Checks whether a CSS selector string references an attribute with the given [name].
 *
 * This is a simplified version of the JS svgo `includesAttrSelector` that checks
 * whether any parsed sub-selector contains an attribute selector matching [name].
 *
 * @param selector The CSS selector string to inspect.
 * @param name The attribute name to look for.
 * @return true if the selector contains an attribute selector referencing [name].
 */
fun includesAttrSelector(selector: String, name: String): Boolean {
    val selectorItems = try {
        parseSelectorListItems(selector)
    } catch (_: Exception) {
        return false
    }

    for (item in selectorItems) {
        for (sel in item.selectors) {
            if (sel is dev.tonholo.kss.parser.ast.css.syntax.node.Selector.Attribute &&
                sel.name == name
            ) {
                return true
            }
        }
    }

    return false
}
