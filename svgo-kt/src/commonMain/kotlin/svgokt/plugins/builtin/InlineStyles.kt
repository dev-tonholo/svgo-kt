package svgokt.plugins.builtin

import svgokt.domain.XastCdata
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastText
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.domain.plugins.VisitorRoot
import svgokt.plugins.xast.detachFromParent
import svgokt.xast.querySelectorAll

/**
 * Inlines CSS rules from <style> elements into inline style attributes.
 *
 * Simplified version that:
 * - Collects <style> elements and parses their CSS using simple regex-based parsing
 * - For each rule, finds matching elements using querySelectorAll
 * - If onlyMatchedOnce is true, only inlines rules that match exactly one element
 * - Merges declarations into the element's style attribute
 * - Removes inlined rules from the stylesheet; removes empty <style> elements
 */
class InlineStyles(
    override val params: Params = Params(),
) : Plugin<InlineStyles.Params> {

    data class Params(
        val onlyMatchedOnce: Boolean = true,
        val removeMatchedSelectors: Boolean = true,
        val useMqs: List<String> = listOf("", "screen"),
        val usePseudos: List<String> = listOf(""),
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "onlyMatchedOnce" to onlyMatchedOnce,
            "removeMatchedSelectors" to removeMatchedSelectors,
            "useMqs" to useMqs,
            "usePseudos" to usePseudos,
        )

    override val name: String = "inlineStyles"
    override val description: String = "inline styles (additional options)"

    override val fn: PluginFn = { root, _, _ ->
        val styleEntries = mutableListOf<StyleEntry>()

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    collectStyleElement(
                        node = node,
                        parentNode = parentNode,
                        styleEntries = styleEntries,
                    )
                },
            ),
            root = VisitorRoot(
                onExit = { rootNode, _ ->
                    processStyles(
                        root = rootNode,
                        styleEntries = styleEntries,
                    )
                },
            ),
        )
    }

    @Suppress("ReturnCount")
    private fun collectStyleElement(
        node: XastElement,
        parentNode: XastParent?,
        styleEntries: MutableList<StyleEntry>,
    ): VisitState {
        if (node.name == "foreignObject") {
            return VisitState.Skip
        }
        if (node.name != "style" || node.children.isEmpty()) {
            return VisitState.Continue
        }

        val type = node.attributes["type"]
        if (type != null && type != "" && type != "text/css") {
            return VisitState.Continue
        }

        val cssText = node.children.mapNotNull { child ->
            when (child) {
                is XastText -> child.value
                is XastCdata -> child.value
                else -> null
            }
        }.joinToString(separator = "")

        if (cssText.isBlank()) {
            return VisitState.Continue
        }

        val rules = parseSimpleCssRules(cssText)
        styleEntries.add(
            StyleEntry(
                node = node,
                parentNode = parentNode,
                rules = rules,
                originalCss = cssText,
            ),
        )

        return VisitState.Continue
    }

    @Suppress(
        "ReturnCount",
        "NestedBlockDepth",
        "CyclomaticComplexMethod",
        "LoopWithTooManyJumpStatements",
        "SwallowedException",
    )
    private fun processStyles(
        root: svgokt.domain.XastRoot,
        styleEntries: MutableList<StyleEntry>,
    ) {
        if (styleEntries.isEmpty()) {
            return
        }

        for (entry in styleEntries) {
            val rulesToRemove = mutableListOf<CssRule>()

            for (rule in entry.rules) {
                // Skip rules with pseudo-classes/elements unless included in usePseudos
                if (rule.selector.contains(':')) {
                    val pseudoPart = extractPseudo(rule.selector)
                    if (pseudoPart !in params.usePseudos) {
                        continue
                    }
                }

                // Skip @ rules (media queries, etc.) unless in useMqs
                if (rule.selector.startsWith('@')) {
                    continue
                }

                val matchedElements = try {
                    querySelectorAll(node = root, selector = rule.selector)
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    continue
                }

                if (matchedElements.isEmpty()) {
                    continue
                }

                if (params.onlyMatchedOnce && matchedElements.size > 1) {
                    continue
                }

                for (element in matchedElements) {
                    mergeDeclarations(element = element, declarations = rule.declarations)
                }

                if (params.removeMatchedSelectors) {
                    rulesToRemove.add(rule)
                }
            }

            entry.rules.removeAll(rulesToRemove.toSet())

            if (entry.rules.isEmpty()) {
                entry.parentNode?.let { entry.node.detachFromParent(it) }
            } else {
                updateStyleContent(entry)
            }
        }
    }

    private fun mergeDeclarations(element: XastElement, declarations: String) {
        val existingStyle = element.attributes["style"]
        if (existingStyle.isNullOrBlank()) {
            element.attributes["style"] = declarations.trim()
        } else {
            // Existing inline styles have higher priority.
            // Parse existing declarations to check for conflicts.
            val existingProps = parseDeclarationProperties(existingStyle)
            val newProps = parseDeclarationProperties(declarations)

            val merged = StringBuilder()
            for ((prop, value) in newProps) {
                if (prop !in existingProps || value.contains("!important")) {
                    merged.append("$prop:$value;")
                }
            }
            merged.append(existingStyle.trimEnd(';'))

            element.attributes["style"] = merged.toString().trimEnd(';')
        }
    }

    private fun parseDeclarationProperties(declarations: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        declarations.split(';').forEach { decl ->
            val trimmed = decl.trim()
            if (trimmed.isNotEmpty()) {
                val colonIndex = trimmed.indexOf(':')
                if (colonIndex > 0) {
                    val property = trimmed.substring(startIndex = 0, endIndex = colonIndex).trim().lowercase()
                    val value = trimmed.substring(startIndex = colonIndex + 1).trim()
                    result[property] = value
                }
            }
        }
        return result
    }

    private fun updateStyleContent(entry: StyleEntry) {
        val newCss = entry.rules.joinToString(separator = "") { rule ->
            "${rule.selector}{${rule.declarations}}"
        }
        val firstChild = entry.node.children.firstOrNull()
        val newChild = when (firstChild) {
            is XastCdata -> XastCdata(value = newCss)
            else -> XastText(value = newCss)
        }
        entry.node.children.clear()
        entry.node.children.add(newChild)
    }

    private fun extractPseudo(selector: String): String {
        val colonIndex = selector.indexOf(':')
        return if (colonIndex >= 0) selector.substring(startIndex = colonIndex) else ""
    }

    private data class StyleEntry(
        val node: XastElement,
        val parentNode: XastParent?,
        val rules: MutableList<CssRule>,
        val originalCss: String,
    )

    internal data class CssRule(
        val selector: String,
        val declarations: String,
    )

    companion object {
        /**
         * Simple CSS rule parser that extracts selector + declarations blocks.
         * Handles basic rules but not nested @ rules or complex selectors with braces.
         */
        @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
        private fun parseSimpleCssRules(css: String): MutableList<CssRule> {
            val rules = mutableListOf<CssRule>()

            // Remove comments first
            val cleaned = css.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")

            var i = 0
            while (i < cleaned.length) {
                // Skip whitespace
                while (i < cleaned.length && cleaned[i].isWhitespace()) i++
                if (i >= cleaned.length) break

                // Skip @ rules (media queries, etc.)
                if (cleaned[i] == '@') {
                    val braceIndex = cleaned.indexOf('{', startIndex = i)
                    if (braceIndex < 0) break
                    // Find matching closing brace (handles one level of nesting)
                    var depth = 1
                    var j = braceIndex + 1
                    while (j < cleaned.length && depth > 0) {
                        if (cleaned[j] == '{') depth++
                        if (cleaned[j] == '}') depth--
                        j++
                    }
                    i = j
                    continue
                }

                // Find the opening brace
                val braceIndex = cleaned.indexOf('{', startIndex = i)
                if (braceIndex < 0) break

                val selector = cleaned.substring(startIndex = i, endIndex = braceIndex).trim()

                // Find matching closing brace
                val closingBrace = cleaned.indexOf('}', startIndex = braceIndex + 1)
                if (closingBrace < 0) break

                val declarations = cleaned.substring(
                    startIndex = braceIndex + 1,
                    endIndex = closingBrace,
                ).trim()

                if (selector.isNotEmpty() && declarations.isNotEmpty()) {
                    // Handle comma-separated selectors: split into individual rules
                    selector.split(',').forEach { sel ->
                        val trimmedSel = sel.trim()
                        if (trimmedSel.isNotEmpty()) {
                            rules.add(CssRule(selector = trimmedSel, declarations = declarations))
                        }
                    }
                }

                i = closingBrace + 1
            }

            return rules
        }
    }
}
