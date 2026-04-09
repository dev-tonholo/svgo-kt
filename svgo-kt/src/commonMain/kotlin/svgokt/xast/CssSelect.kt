package svgokt.xast

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent

/**
 * Finds all [XastElement]s within the given [node] that match the CSS [selector].
 *
 * @param node The root of the tree to search in.
 * @param selector A CSS selector string (e.g. `rect`, `.cls`, `#id`).
 * @param parents Optional mapping of child nodes to their parents, used for
 *   combinator-aware matching.
 * @return All matching elements, in document order.
 */
fun querySelectorAll(
    node: XastParent,
    selector: String,
    parents: Map<XastChild, XastParent>? = null,
): List<XastElement> {
    val resolvedSelectors = parseResolvedSelectors(selector)
    return collectElements(node).filter { element ->
        resolvedSelectors.any { resolved ->
            matchesSelectorListItem(
                element = element,
                selectorItem = resolved.base,
                parents = parents,
            ) && resolved.negations.none { negation ->
                matchesSelectorListItem(
                    element = element,
                    selectorItem = negation,
                    parents = parents,
                )
            }
        }
    }
}

/**
 * Finds the first [XastElement] within the given [node] that matches the CSS [selector].
 *
 * @param node The root of the tree to search in.
 * @param selector A CSS selector string (e.g. `rect`, `.cls`, `#id`).
 * @param parents Optional mapping of child nodes to their parents, used for
 *   combinator-aware matching.
 * @return The first matching element, or `null` if none match.
 */
fun querySelector(
    node: XastParent,
    selector: String,
    parents: Map<XastChild, XastParent>? = null,
): XastElement? {
    val resolvedSelectors = parseResolvedSelectors(selector)
    return collectElements(node).firstOrNull { element ->
        resolvedSelectors.any { resolved ->
            matchesSelectorListItem(
                element = element,
                selectorItem = resolved.base,
                parents = parents,
            ) && resolved.negations.none { negation ->
                matchesSelectorListItem(
                    element = element,
                    selectorItem = negation,
                    parents = parents,
                )
            }
        }
    }
}

/**
 * Checks whether the given [node] matches the CSS [selector].
 *
 * @param node The element to test.
 * @param selector A CSS selector string (e.g. `rect`, `.cls`, `#id`).
 * @param parents Optional mapping of child nodes to their parents, used for
 *   combinator-aware matching.
 * @return `true` if [node] matches [selector], `false` otherwise.
 */
fun matches(
    node: XastElement,
    selector: String,
    parents: Map<XastChild, XastParent>? = null,
): Boolean {
    val resolvedSelectors = parseResolvedSelectors(selector)
    return resolvedSelectors.any { resolved ->
        matchesSelectorListItem(
            element = node,
            selectorItem = resolved.base,
            parents = parents,
        ) && resolved.negations.none { negation ->
            matchesSelectorListItem(
                element = node,
                selectorItem = negation,
                parents = parents,
            )
        }
    }
}

/**
 * Collects all [XastElement] descendants of [node] in document order,
 * including direct children and nested elements.
 */
private fun collectElements(node: XastParent): List<XastElement> =
    buildList {
        for (child in node.children) {
            if (child is XastElement) {
                add(child)
                addAll(collectElements(node = child))
            }
        }
    }
