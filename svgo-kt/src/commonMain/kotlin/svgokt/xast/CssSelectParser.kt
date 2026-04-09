package svgokt.xast

import dev.tonholo.kss.lexer.css.CssTokenizer
import dev.tonholo.kss.parser.ast.css.CssCombinator
import dev.tonholo.kss.parser.ast.css.CssParser
import dev.tonholo.kss.parser.ast.css.consumer.CssConsumers
import dev.tonholo.kss.parser.ast.css.syntax.node.Prelude
import dev.tonholo.kss.parser.ast.css.syntax.node.QualifiedRule
import dev.tonholo.kss.parser.ast.css.syntax.node.Selector
import dev.tonholo.kss.parser.ast.css.syntax.node.SelectorListItem
import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent

/**
 * CSS combinators that the selector engine does not support. Selectors
 * containing any of these tokens are rejected early so that the kss
 * tokenizer does not hang or loop on non-standard syntax.
 */
private val UNSUPPORTED_COMBINATOR_TOKENS = listOf("/deep/", ">>>", "::shadow")

/**
 * Regex that matches a single `:not(...)` functional pseudo-class.
 * Captures the content inside the parentheses as group 1.
 */
private val NOT_PSEUDO_REGEX = Regex(":not\\(([^)]+)\\)")

/**
 * Wraps a parsed [SelectorListItem] together with optional negation selectors
 * extracted from `:not()` pseudo-classes that appear in the original CSS text.
 */
internal data class ResolvedSelector(
    val base: SelectorListItem,
    val negations: List<SelectorListItem>,
)

/**
 * Parses a CSS selector string into a list of [ResolvedSelector]s.
 *
 * Selectors are delegated to the kss parser via a fake rule (`selector { }`).
 *
 * `:not()` pseudo-classes are extracted and parsed separately so the negation
 * logic can be applied during matching without relying on the kss parser's
 * PseudoClass node.
 *
 * Comma-separated groups are split before parsing so each group can be
 * handled independently.
 */
internal fun parseResolvedSelectors(selector: String): List<ResolvedSelector> =
    selector.split(",").mapNotNull { part ->
        val trimmed = part.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        if (UNSUPPORTED_COMBINATOR_TOKENS.any { token -> trimmed.contains(token) }) {
            return@mapNotNull null
        }
        resolveSelector(trimmed)
    }

/**
 * Parses a single (non-comma-separated) selector, extracting `:not()` into
 * separate negation selectors for manual evaluation.
 */
@Suppress("ReturnCount")
private fun resolveSelector(selector: String): ResolvedSelector? {
    val notMatches = NOT_PSEUDO_REGEX.findAll(selector).toList()
    if (notMatches.isEmpty()) {
        val base = tryParseViaKss(selector) ?: return null
        return ResolvedSelector(base = base, negations = emptyList())
    }

    // Strip all :not(...) occurrences to get the base selector.
    val baseSelectorText = NOT_PSEUDO_REGEX.replace(selector, "").trim().ifEmpty { "*" }
    val base = tryParseViaKss(baseSelectorText) ?: return null

    val negations = notMatches.mapNotNull { match ->
        val inner = match.groupValues[1].trim()
        tryParseViaKss(inner)
    }

    return ResolvedSelector(base = base, negations = negations)
}

/**
 * Legacy entry point that returns plain [SelectorListItem]s.
 * Kept for callers that do not need `:not()` support.
 */
internal fun parseSelectorListItems(selector: String): List<SelectorListItem> =
    selector.split(",").mapNotNull { part ->
        val trimmed = part.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        // Bail out early for unsupported combinators that can cause the
        // parser to hang (e.g. /deep/, >>>, ::shadow).
        if (UNSUPPORTED_COMBINATOR_TOKENS.any { token -> trimmed.contains(token) }) {
            return@mapNotNull null
        }
        tryParseViaKss(trimmed)
    }

/**
 * Parses [selector] by wrapping it in a fake rule and using the kss CSS parser.
 * Returns `null` when the kss parser rejects the input.
 */
@Suppress("ReturnCount")
internal fun tryParseViaKss(selector: String): SelectorListItem? {
    return try {
        val fakeRule = "$selector { }"
        val tokenizer = CssTokenizer()
        val tokens = tokenizer.tokenize(input = fakeRule)
        val consumers = CssConsumers(content = fakeRule)
        val parser = CssParser(consumers = consumers)
        val styleSheet = parser.parse(tokens = tokens)
        val rule = styleSheet.children.filterIsInstance<QualifiedRule>().firstOrNull()
            ?: return null
        val prelude = rule.prelude as? Prelude.Selector ?: return null
        prelude.components.firstOrNull()
    } catch (_: IllegalStateException) {
        null
    }
}


/**
 * Checks whether [element] matches a single [SelectorListItem], which is a
 * compound selector (e.g. `rect.cls`).
 *
 * For compound selectors with descendant/child combinators the [parents] map
 * is consulted to verify the structural relationship.
 */
@Suppress("ReturnCount")
internal fun matchesSelectorListItem(
    element: XastElement,
    selectorItem: SelectorListItem,
    parents: Map<XastChild, XastParent>?,
): Boolean {
    val simpleSelectors = selectorItem.selectors
    if (simpleSelectors.isEmpty()) return false

    // Walk selectors right-to-left so the rightmost part is matched against the
    // element itself and earlier parts (with combinators) are matched against
    // the element's ancestors.
    val lastIndex = simpleSelectors.lastIndex
    var currentElement: XastElement? = element

    for (i in lastIndex downTo 0) {
        val sel = simpleSelectors[i]
        val target = currentElement ?: return false

        if (!matchesSimpleSelector(element = target, selector = sel)) return false

        // If this selector carries a combinator it determines the relationship
        // to the next (more leftward) selector.
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

/**
 * Resolves the parent/ancestor of [element] that should be tested against the
 * next leftward selector given a [combinator].
 *
 * Only [CssCombinator.ChildCombinator] (direct parent) and
 * [CssCombinator.DescendantCombinator] (any ancestor) are supported;
 * sibling combinators always return `null`.
 */
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
 * Returns `true` when [element] matches a single simple [selector].
 */
internal fun matchesSimpleSelector(
    element: XastElement,
    selector: Selector,
): Boolean = when (selector) {
    is Selector.Type -> selector.name == "*" || selector.name == element.name
    is Selector.Class -> {
        val classAttr = element.attributes["class"] ?: return false
        val classes = classAttr.trim().split(regex = Regex("\\s+"))
        selector.name in classes
    }
    is Selector.Id -> element.attributes["id"] == selector.name
    is Selector.Attribute -> {
        val attrValue = element.attributes[selector.name]
        val matcher = selector.matcher
        val value = selector.value
        when {
            matcher == null -> attrValue != null
            value == null -> attrValue != null
            else -> matchesAttributeValue(
                attrValue = attrValue,
                matcher = matcher,
                expected = value,
            )
        }
    }
    // Pseudo-classes like :not() are handled at the string level by
    // parseResolvedSelectors before reaching this matching function.
    // Any remaining pseudo-class selectors that reach here are unsupported.
    is Selector.PseudoClass -> false
    is Selector.PseudoElement -> false
}

/**
 * Checks whether [attrValue] satisfies the CSS attribute [matcher] against [expected].
 *
 * Supported matchers:
 * - `=`  exact match
 * - `~=` word match (space-separated list)
 * - `|=` dash-prefixed match (exactly [expected] or starts with "[expected]-")
 * - `^=` starts with [expected]
 * - `$=` ends with [expected]
 * - `*=` contains [expected]
 */
private fun matchesAttributeValue(
    attrValue: String?,
    matcher: String,
    expected: String,
): Boolean {
    attrValue ?: return false
    return when (matcher) {
        "=" -> attrValue == expected
        "~=" -> expected in attrValue.trim().split(regex = Regex("\\s+"))
        "|=" -> attrValue == expected || attrValue.startsWith(prefix = "$expected-")
        "^=" -> attrValue.startsWith(prefix = expected)
        "$=" -> attrValue.endsWith(suffix = expected)
        "*=" -> attrValue.contains(other = expected)
        else -> false
    }
}
