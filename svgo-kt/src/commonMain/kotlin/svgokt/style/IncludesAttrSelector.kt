package svgokt.style

/**
 * Regex matching CSS attribute selectors, e.g. `[version]`, `[version="1.1"]`.
 * Captures the attribute name in group 1.
 */
private val attrSelectorRegex = Regex("""\[([a-zA-Z_][\w-]*)(?:[~|^$*]?=)?""")

/**
 * Checks whether a CSS selector string references an attribute with the given [name].
 *
 * Uses regex matching instead of a full CSS selector parser to avoid
 * dependency on kss parser internals that may change.
 *
 * @param selector The CSS selector string to inspect.
 * @param name The attribute name to look for.
 * @return true if the selector contains an attribute selector referencing [name].
 */
fun includesAttrSelector(selector: String, name: String): Boolean {
    for (match in attrSelectorRegex.findAll(selector)) {
        if (match.groupValues[1] == name) {
            return true
        }
    }
    return false
}
