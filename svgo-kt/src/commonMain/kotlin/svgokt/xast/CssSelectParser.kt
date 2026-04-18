package svgokt.xast

import dev.tonholo.kss.parser.ast.css.CssCombinator
import dev.tonholo.kss.parser.ast.css.CssSelectorParser
import dev.tonholo.kss.parser.ast.css.syntax.node.Selector
import dev.tonholo.kss.parser.ast.css.syntax.node.SelectorListItem
import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent

private val selectorParser = CssSelectorParser()

/**
 * Parses a CSS selector string into a list of [SelectorListItem].
 * Returns an empty list when the input cannot be parsed.
 */
internal fun parseSelectors(selector: String): List<SelectorListItem> =
    selectorParser.parse(selector = selector)

/**
 * Checks whether [element] matches a compound [selectorItem], walking
 * right-to-left through combinator chains using the [parents] map.
 */
internal fun matchesSelectorListItem(
    element: XastElement,
    selectorItem: SelectorListItem,
    parents: Map<XastChild, XastParent>?,
): Boolean {
    val selectors = selectorItem.selectors
    if (selectors.isEmpty()) return false

    var currentElement: XastElement? = element
    for (i in selectors.lastIndex downTo 0) {
        val sel = selectors[i]
        val target = currentElement ?: return false
        if (!matchesSimpleSelector(element = target, selector = sel)) return false

        val combinator = sel.combinator
        if (i > 0 && combinator != null) {
            currentElement = resolveAncestor(
                element = target,
                combinator = combinator,
                parents = parents,
            )
        }
    }
    return true
}

private fun resolveAncestor(
    element: XastElement,
    combinator: CssCombinator,
    parents: Map<XastChild, XastParent>?,
): XastElement? {
    val parent = parents?.get(element) as? XastElement ?: return null
    return when (combinator) {
        CssCombinator.ChildCombinator -> parent
        CssCombinator.DescendantCombinator -> parent
        else -> null
    }
}

/**
 * Matches a single simple selector against an XAST element.
 */
internal fun matchesSimpleSelector(
    element: XastElement,
    selector: Selector,
): Boolean = when (selector) {
    is Selector.Type -> selector.name == "*" || selector.name == element.name
    is Selector.Class -> {
        val classes = element.attributes["class"]?.trim()?.split(Regex("\\s+")).orEmpty()
        selector.name in classes
    }
    is Selector.Id -> element.attributes["id"] == selector.name
    is Selector.Attribute -> matchesAttribute(element, selector)
    is Selector.PseudoClass -> when (selector.name) {
        "not" -> selector.parameters.none { matchesSimpleSelector(element, it) }
        else -> false
    }
    is Selector.PseudoElement -> false
}

private fun matchesAttribute(element: XastElement, selector: Selector.Attribute): Boolean {
    val attrValue = element.attributes[selector.name]
    val matcher = selector.matcher
    val expected = selector.value
    if (matcher == null || expected == null) return attrValue != null
    attrValue ?: return false
    return when (matcher) {
        "=" -> attrValue == expected
        "~=" -> expected in attrValue.trim().split(Regex("\\s+"))
        "|=" -> attrValue == expected || attrValue.startsWith("$expected-")
        "^=" -> attrValue.startsWith(expected)
        "$=" -> attrValue.endsWith(expected)
        "*=" -> attrValue.contains(expected)
        else -> false
    }
}
