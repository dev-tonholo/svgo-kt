package svgokt.xast

import dev.tonholo.kss.lexer.css.CssTokenizer
import dev.tonholo.kss.parser.ast.css.CssCombinator
import dev.tonholo.kss.parser.ast.css.CssParser
import dev.tonholo.kss.parser.ast.css.consumer.CssConsumers
import dev.tonholo.kss.parser.ast.css.syntax.node.QualifiedRule
import dev.tonholo.kss.parser.ast.css.syntax.node.Selector
import dev.tonholo.kss.parser.ast.css.syntax.node.SelectorListItem
import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent

// region kss workarounds
// The following workarounds exist because kss does not yet provide:
//   1. A direct `parseSelector(String)` API - we wrap in a fake rule as a workaround
//      (kss issue: needs SelectorConsumer that accepts raw selector strings)
//   2. Graceful handling of non-standard combinators (/deep/, >>>, ::shadow) -
//      kss hangs on these, so we bail out before parsing
//      (kss issue: tokenizer should reject or skip unknown combinators)
//   3. Functional pseudo-class evaluation (:not(), :is(), :has()) - kss parses
//      them as PseudoClass nodes but does not expose the inner selector list
//      in a usable form for matching
//      (kss issue: PseudoClass.parameters should contain parsed Selector nodes)
// These should be removed once kss provides native support.

/**
 * Non-standard CSS combinators that kss cannot handle without hanging.
 */
private val UNSUPPORTED_COMBINATOR_TOKENS = listOf("/deep/", ">>>", "::shadow")

/**
 * Regex to extract `:not(...)` content. Needed because kss parses `:not()`
 * as a PseudoClass but does not expose the inner selectors for matching.
 */
private val NOT_PSEUDO_REGEX = Regex(":not\\(([^)]+)\\)")

/**
 * Parses [selector] using kss by wrapping it in a fake rule.
 * This is a workaround for kss not having a direct selector parsing API.
 *
 * @return parsed [SelectorListItem], or `null` when kss rejects the input.
 */
internal fun parseViaKss(selector: String): SelectorListItem? {
    return try {
        val fakeRule = "$selector { }"
        val tokenizer = CssTokenizer()
        val tokens = tokenizer.tokenize(input = fakeRule)
        val consumers = CssConsumers(content = fakeRule)
        val parser = CssParser(consumers = consumers)
        val styleSheet = parser.parse(tokens = tokens)
        val rule = styleSheet.children.filterIsInstance<QualifiedRule>().firstOrNull()
            ?: return null
        rule.prelude.components.firstOrNull()
    } catch (_: IllegalStateException) {
        null
    }
}

// endregion

/**
 * A parsed CSS selector with optional `:not()` negations extracted separately.
 */
internal data class ResolvedSelector(
    val base: SelectorListItem,
    val negations: List<SelectorListItem>,
)

/**
 * Parses a comma-separated CSS selector string into [ResolvedSelector]s.
 * Extracts `:not()` pseudo-classes for separate negation matching.
 */
internal fun parseResolvedSelectors(selector: String): List<ResolvedSelector> =
    selector.split(",").mapNotNull { part ->
        val trimmed = part.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        // kss workaround: bail out for non-standard combinators that cause hangs
        if (UNSUPPORTED_COMBINATOR_TOKENS.any { it in trimmed }) return@mapNotNull null
        resolveSelector(trimmed)
    }

/**
 * Parses a single selector group, extracting `:not()` into negation selectors.
 */
private fun resolveSelector(selector: String): ResolvedSelector? {
    val notMatches = NOT_PSEUDO_REGEX.findAll(selector).toList()
    if (notMatches.isEmpty()) {
        val base = parseViaKss(selector) ?: return null
        return ResolvedSelector(base = base, negations = emptyList())
    }

    val baseSelectorText = NOT_PSEUDO_REGEX.replace(selector, "").trim().ifEmpty { "*" }
    val base = parseViaKss(baseSelectorText) ?: return null
    val negations = notMatches.mapNotNull { match ->
        parseViaKss(match.groupValues[1].trim())
    }
    return ResolvedSelector(base = base, negations = negations)
}

/**
 * Parses a comma-separated CSS selector into plain [SelectorListItem]s.
 * For callers that do not need `:not()` support.
 */
internal fun parseSelectorListItems(selector: String): List<SelectorListItem> =
    selector.split(",").mapNotNull { part ->
        val trimmed = part.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        if (UNSUPPORTED_COMBINATOR_TOKENS.any { it in trimmed }) return@mapNotNull null
        parseViaKss(trimmed)
    }

// region XAST matching

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
    is Selector.PseudoClass -> false // :not() handled upstream; others unsupported
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

// endregion
