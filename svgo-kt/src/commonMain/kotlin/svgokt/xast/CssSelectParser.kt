package svgokt.xast

import dev.tonholo.kss.lexer.css.CssTokenizer
import dev.tonholo.kss.parser.ast.css.CssCombinator
import dev.tonholo.kss.parser.ast.css.CssParser
import dev.tonholo.kss.parser.ast.css.consumer.CssConsumers
import dev.tonholo.kss.parser.ast.css.syntax.node.CssLocation
import dev.tonholo.kss.parser.ast.css.syntax.node.Prelude
import dev.tonholo.kss.parser.ast.css.syntax.node.QualifiedRule
import dev.tonholo.kss.parser.ast.css.syntax.node.Selector
import dev.tonholo.kss.parser.ast.css.syntax.node.SelectorListItem
import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent

/**
 * Parses a CSS selector string into a list of [SelectorListItem]s.
 *
 * Selectors that start with a type name or class/id/pseudo-class prefix are
 * delegated to the kss parser via a fake rule (`selector { }`). Selectors
 * starting with `*` or `[` are handled by a lightweight fallback parser
 * because the kss tokenizer does not recognise these characters at position 0.
 *
 * Comma-separated groups are split before parsing so each group can be
 * handled independently.
 */
internal fun parseSelectorListItems(selector: String): List<SelectorListItem> =
    selector.split(",").mapNotNull { part ->
        val trimmed = part.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        tryParseViaKss(trimmed) ?: parseFallback(trimmed)
    }

/**
 * Attempts to parse [selector] by wrapping it in a fake rule and using the
 * kss CSS parser. Returns `null` when the kss tokenizer rejects the input
 * (e.g. when the selector starts with `*` or `[`).
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
 * A lightweight fallback that hand-parses simple selectors the kss tokenizer
 * cannot handle at position 0 (universal selector `*` and attribute selectors
 * `[attr]` / `[attr=value]`).
 *
 * Compound selectors such as `*.cls` or `[disabled].active` are supported
 * by splitting on simple-selector boundaries.
 */
@Suppress("ReturnCount")
internal fun parseFallback(selector: String): SelectorListItem? {
    val simpleSelectors = mutableListOf<Selector>()
    var remaining = selector.trim()

    while (remaining.isNotEmpty()) {
        when {
            remaining.startsWith("*") -> {
                simpleSelectors += Selector.Type(
                    location = CssLocation.Undefined,
                    name = "*",
                )
                remaining = remaining.removePrefix("*")
            }

            remaining.startsWith("[") -> {
                val closeBracket = remaining.indexOf(']')
                if (closeBracket < 0) return null
                val attrExpr = remaining.substring(startIndex = 1, endIndex = closeBracket)
                simpleSelectors += parseAttributeSelector(attrExpr) ?: return null
                remaining = remaining.substring(startIndex = closeBracket + 1)
            }

            else -> {
                // Hand off any remaining tail (e.g. `.cls` after `*`) to kss.
                val tailItem = tryParseViaKss(remaining) ?: return null
                simpleSelectors.addAll(tailItem.selectors)
                remaining = ""
            }
        }
    }

    if (simpleSelectors.isEmpty()) return null

    return SelectorListItem(
        location = CssLocation.Undefined,
        selectors = simpleSelectors,
    )
}

/** Parses `attr`, `attr=value`, `attr~=value`, etc. from inside `[...]`. */
@Suppress("ReturnCount")
internal fun parseAttributeSelector(attrExpr: String): Selector? {
    // Look for matcher operators in order of longest-first to avoid prefix conflicts.
    val matchers = listOf("~=", "|=", "^=", "$=", "*=", "=")
    for (matcher in matchers) {
        val idx = attrExpr.indexOf(matcher)
        if (idx >= 0) {
            val name = attrExpr.substring(startIndex = 0, endIndex = idx).trim()
            val rawValue = attrExpr.substring(startIndex = idx + matcher.length).trim()
            val value = rawValue.removeSurrounding("\"").removeSurrounding("'")
            return Selector.Attribute(
                location = CssLocation.Undefined,
                name = name,
                matcher = matcher,
                value = value,
            )
        }
    }
    // Presence-only selector: [attr]
    val name = attrExpr.trim()
    if (name.isEmpty()) return null
    return Selector.Attribute(
        location = CssLocation.Undefined,
        name = name,
    )
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
