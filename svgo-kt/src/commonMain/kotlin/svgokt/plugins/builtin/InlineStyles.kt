@file:Suppress("TooManyFunctions")

package svgokt.plugins.builtin

import svgokt.domain.XastCdata
import svgokt.domain.XastElement
import svgokt.domain.XastRoot
import svgokt.domain.XastText
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.domain.plugins.VisitorRoot
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent
import svgokt.xast.querySelectorAll

/**
 * Inlines CSS rules from <style> elements into inline style attributes.
 *
 * Follows the JS reference implementation closely:
 * - Collects <style> elements and parses their CSS
 * - Sorts selectors by specificity (highest first)
 * - For each selector, finds matching elements
 * - If onlyMatchedOnce, skips selectors matching multiple elements
 * - Merges declarations into element style attributes
 * - Removes inlined selectors from stylesheets
 * - Cleans up class/id attributes when no longer needed
 * - Removes empty <style> elements
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

    override val fn: PluginFn = { root, params, _ ->
        val resolvedParams = resolveParams(params)
        val styleEntries = mutableListOf<StyleEntry>()

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    collectStyleElement(
                        node = node,
                        parentNode = parentNode,
                        styleEntries = styleEntries,
                        resolvedParams = resolvedParams,
                    )
                },
            ),
            root = VisitorRoot(
                onExit = { rootNode, _ ->
                    processStyles(
                        root = rootNode,
                        styleEntries = styleEntries,
                        resolvedParams = resolvedParams,
                    )
                },
            ),
        )
    }

    @Suppress("ReturnCount")
    private fun collectStyleElement(
        node: XastElement,
        parentNode: svgokt.domain.XastParent?,
        styleEntries: MutableList<StyleEntry>,
        resolvedParams: ResolvedParams,
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

        val parseResult = parseStylesheet(
            css = cssText,
            useMqs = resolvedParams.useMqs,
            usePseudos = resolvedParams.usePseudos,
        )

        styleEntries.add(
            StyleEntry(
                node = node,
                parentNode = parentNode,
                selectors = parseResult.selectors,
                atRules = parseResult.atRules,
                skippedRules = parseResult.skippedRules,
                originalCss = cssText,
            ),
        )

        return VisitState.Continue
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    private fun processStyles(
        root: XastRoot,
        styleEntries: MutableList<StyleEntry>,
        resolvedParams: ResolvedParams,
    ) {
        if (styleEntries.isEmpty()) return

        // Collect all selectors from all style entries, sorted by specificity (highest first)
        val allSelectors = mutableListOf<SelectorWithEntry>()
        for (entry in styleEntries) {
            for (selector in entry.selectors) {
                allSelectors.add(SelectorWithEntry(selector = selector, entry = entry))
            }
        }

        // Sort by specificity, highest first (matching JS: sort ascending then reverse)
        allSelectors.sortWith(compareBy<SelectorWithEntry> { it.selector.specificity.a }
            .thenBy { it.selector.specificity.b }
            .thenBy { it.selector.specificity.c })
        allSelectors.reverse()

        val allSelectorItems = allSelectors.map { it.selector }

        for (selectorWithEntry in allSelectors) {
            val selector = selectorWithEntry.selector
            val selectorText = selector.matchSelector

            val matchedElements = try {
                querySelectorAll(node = root, selector = selectorText)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                continue
            }

            if (matchedElements.isEmpty()) continue
            if (resolvedParams.onlyMatchedOnce && matchedElements.size > 1) continue

            // Apply styles to matched elements
            for (element in matchedElements) {
                mergeDeclarations(
                    element = element,
                    ruleDeclarations = selector.declarations,
                    allSelectors = allSelectorItems,
                )
            }

            if (resolvedParams.removeMatchedSelectors) {
                selector.removed = true
            }
            selector.matchedElements = matchedElements
        }

        if (!resolvedParams.removeMatchedSelectors) return

        // Clean up matched class + ID attribute values
        for (selectorWithEntry in allSelectors) {
            val selector = selectorWithEntry.selector
            val matchedEls = selector.matchedElements ?: continue
            if (resolvedParams.onlyMatchedOnce && matchedEls.size > 1) continue

            for (element in matchedEls) {
                cleanupClassAndId(
                    element = element,
                    selector = selector,
                    allSelectors = allSelectorItems,
                )
            }
        }

        // Update style elements
        for (entry in styleEntries) {
            val newCss = rebuildStylesheet(entry)
            if (newCss.isEmpty()) {
                entry.parentNode?.let { entry.node.detachFromParent(it) }
            } else {
                val firstChild = entry.node.children.firstOrNull()
                val newChild = when (firstChild) {
                    is XastCdata -> XastCdata(value = newCss)
                    else -> XastText(value = newCss)
                }
                entry.node.children.clear()
                entry.node.children.add(newChild)
            }
        }
    }

    companion object {
        private val PRESERVED_PSEUDOS = setOf(
            // Functional pseudo-classes
            "is", "not", "where", "has",
            "nth-child", "nth-last-child", "nth-of-type", "nth-last-of-type",
            // Tree-structural pseudo-classes
            "root", "empty",
            "first-child", "last-child", "only-child",
            "first-of-type", "last-of-type", "only-of-type",
        )

        @Suppress("ReturnCount")
        private fun resolveParams(params: PluginParams): ResolvedParams {
            val onlyMatchedOnce = params["onlyMatchedOnce"] as? Boolean ?: true
            val removeMatchedSelectors = params["removeMatchedSelectors"] as? Boolean ?: true

            @Suppress("UNCHECKED_CAST")
            val useMqs = (params["useMqs"] as? List<String>) ?: listOf("", "screen")

            @Suppress("UNCHECKED_CAST")
            val usePseudos = (params["usePseudos"] as? List<String>) ?: listOf("")

            return ResolvedParams(
                onlyMatchedOnce = onlyMatchedOnce,
                removeMatchedSelectors = removeMatchedSelectors,
                useMqs = useMqs,
                usePseudos = usePseudos,
            )
        }

        /**
         * Merges rule declarations into element's inline style.
         * Existing inline styles have higher priority unless the rule has !important.
         */
        private fun mergeDeclarations(
            element: XastElement,
            ruleDeclarations: List<Declaration>,
            allSelectors: List<CssSelector>,
        ) {
            val existingStyle = element.attributes["style"] ?: ""
            val existingDecls = parseInlineDeclarations(existingStyle)

            val merged = mutableListOf<Declaration>()

            // Add rule declarations first (lower priority)
            for (decl in ruleDeclarations) {
                // Remove presentation attributes if not used in attribute selectors
                if (Collections.presentationAttrs.contains(decl.property)) {
                    val usedInAttrSelector = allSelectors.any { sel ->
                        includesAttrSelector(
                            selectorText = sel.originalSelector,
                            attrName = decl.property,
                        )
                    }
                    if (!usedInAttrSelector) {
                        element.attributes.remove(decl.property)
                    }
                }

                val existingDecl = existingDecls.find { it.property == decl.property }
                if (existingDecl == null) {
                    merged.add(decl)
                } else if (!existingDecl.important && decl.important) {
                    // Rule !important overrides non-important inline
                    existingDecls.removeAll { it.property == decl.property }
                    merged.add(decl)
                }
                // Otherwise existing inline wins
            }

            // Append existing inline declarations
            merged.addAll(existingDecls)

            val newStyle = merged.joinToString(separator = ";") { decl ->
                val importantSuffix = if (decl.important) "!important" else ""
                "${decl.property}:${decl.value}$importantSuffix"
            }
            if (newStyle.isNotEmpty()) {
                element.attributes["style"] = newStyle
            }
        }

        private fun cleanupClassAndId(
            element: XastElement,
            selector: CssSelector,
            allSelectors: List<CssSelector>,
        ) {
            // Clean up classes
            val classAttr = element.attributes["class"]
            if (classAttr != null) {
                val classList = classAttr.split(' ').toMutableSet()
                for (className in selector.classNames) {
                    val usedInAttrSelector = allSelectors.any { sel ->
                        includesAttrSelector(
                            selectorText = sel.originalSelector,
                            attrName = "class",
                            attrValue = className,
                            checkValue = true,
                        )
                    }
                    if (!usedInAttrSelector) {
                        classList.remove(className)
                    }
                }
                if (classList.isEmpty()) {
                    element.attributes.remove("class")
                } else {
                    element.attributes["class"] = classList.joinToString(separator = " ")
                }
            }

            // Clean up ID
            val idName = selector.idName
            if (idName != null && element.attributes["id"] == idName) {
                val usedInAttrSelector = allSelectors.any { sel ->
                    includesAttrSelector(
                        selectorText = sel.originalSelector,
                        attrName = "id",
                        attrValue = idName,
                        checkValue = true,
                    )
                }
                if (!usedInAttrSelector) {
                    element.attributes.remove("id")
                }
            }
        }

        /**
         * Checks if a CSS selector text contains an attribute selector referencing
         * the given attribute name (and optionally value).
         */
        @Suppress("ReturnCount")
        private fun includesAttrSelector(
            selectorText: String,
            attrName: String,
            attrValue: String? = null,
            checkValue: Boolean = false,
        ): Boolean {
            if (!selectorText.contains('[')) return false

            val attrPattern = if (checkValue && attrValue != null) {
                Regex("""\[$attrName[~|^$*]?=["\']?${Regex.escape(attrValue)}""")
            } else {
                Regex("""\[$attrName[\]~|^$*=]""")
            }
            return attrPattern.containsMatchIn(selectorText)
        }

        /**
         * Parses inline style declarations from a style attribute value.
         */
        private fun parseInlineDeclarations(style: String): MutableList<Declaration> {
            val result = mutableListOf<Declaration>()
            if (style.isBlank()) return result

            style.split(';').forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) {
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0) {
                        val prop = trimmed.substring(startIndex = 0, endIndex = colonIdx).trim()
                        var value = trimmed.substring(startIndex = colonIdx + 1).trim()
                        val important = value.contains("!important")
                        if (important) {
                            value = value.replace("!important", "").trim()
                        }
                        result.add(
                            Declaration(
                                property = prop.lowercase(),
                                value = value,
                                important = important,
                            ),
                        )
                    }
                }
            }
            return result
        }

        /**
         * Parses a complete stylesheet into selectors and at-rules.
         * Handles media queries, nested rules, and pseudo-class stripping.
         */
        @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
        private fun parseStylesheet(
            css: String,
            useMqs: List<String>,
            usePseudos: List<String>,
        ): ParseResult {
            val selectors = mutableListOf<CssSelector>()
            val atRules = mutableListOf<AtRule>()
            val skippedRules = mutableListOf<SkippedRule>()

            // Remove comments
            val cleaned = css.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")

            var i = 0
            while (i < cleaned.length) {
                // Skip whitespace
                while (i < cleaned.length && cleaned[i].isWhitespace()) i++
                if (i >= cleaned.length) break

                if (cleaned[i] == '@') {
                    val result = parseAtRule(
                        css = cleaned,
                        startIndex = i,
                        useMqs = useMqs,
                        usePseudos = usePseudos,
                        selectors = selectors,
                        atRules = atRules,
                        skippedRules = skippedRules,
                    )
                    i = result
                    continue
                }

                // Find opening brace
                val braceIdx = cleaned.indexOf('{', startIndex = i)
                if (braceIdx < 0) break

                val selectorText = cleaned.substring(startIndex = i, endIndex = braceIdx).trim()

                // Find closing brace
                val closeIdx = cleaned.indexOf('}', startIndex = braceIdx + 1)
                if (closeIdx < 0) break

                val declarationsText = cleaned.substring(
                    startIndex = braceIdx + 1,
                    endIndex = closeIdx,
                ).trim()

                if (selectorText.isNotEmpty()) {
                    // Default media query is "" (no media query)
                    processSelectorList(
                        selectorText = selectorText,
                        declarationsText = declarationsText,
                        mediaQuery = "",
                        useMqs = useMqs,
                        usePseudos = usePseudos,
                        selectors = selectors,
                        skippedRules = skippedRules,
                    )
                }

                i = closeIdx + 1
            }

            return ParseResult(
                selectors = selectors,
                atRules = atRules,
                skippedRules = skippedRules,
            )
        }

        @Suppress("LongParameterList")
        private fun parseAtRule(
            css: String,
            startIndex: Int,
            useMqs: List<String>,
            usePseudos: List<String>,
            selectors: MutableList<CssSelector>,
            atRules: MutableList<AtRule>,
            skippedRules: MutableList<SkippedRule>,
        ): Int {
            val braceIdx = css.indexOf('{', startIndex = startIndex)
            val semiIdx = css.indexOf(';', startIndex = startIndex)

            // Simple at-rule (no block, ends with ;)
            if (semiIdx >= 0 && (braceIdx < 0 || semiIdx < braceIdx)) {
                val ruleText = css.substring(startIndex = startIndex, endIndex = semiIdx + 1).trim()
                atRules.add(AtRule(text = ruleText))
                return semiIdx + 1
            }

            if (braceIdx < 0) return css.length

            val prelude = css.substring(startIndex = startIndex, endIndex = braceIdx).trim()

            // Find matching closing brace
            var depth = 1
            var j = braceIdx + 1
            while (j < css.length && depth > 0) {
                if (css[j] == '{') depth++
                if (css[j] == '}') depth--
                j++
            }

            val innerCss = css.substring(startIndex = braceIdx + 1, endIndex = j - 1)

            // Check if this is a media rule
            if (prelude.startsWith("@media")) {
                val mediaQuery = buildMediaQuery(prelude)

                // Parse inner rules with this media query context
                if (useMqs.contains(mediaQuery)) {
                    parseInnerRules(
                        innerCss = innerCss,
                        mediaQuery = mediaQuery,
                        useMqs = useMqs,
                        usePseudos = usePseudos,
                        selectors = selectors,
                        skippedRules = skippedRules,
                    )
                    // Store the at-rule shell for rebuilding
                    atRules.add(AtRule(text = prelude, inner = innerCss, isMedia = true))
                } else {
                    // Keep the entire at-rule as-is
                    atRules.add(AtRule(text = prelude, inner = innerCss, isMedia = true))
                }
            } else {
                // Non-media at-rule with block (font-face, keyframes, etc.)
                atRules.add(AtRule(text = prelude, inner = innerCss))
            }

            return j
        }

        private fun buildMediaQuery(prelude: String): String {
            // prelude is like "@media screen" or "@media only screen and (...)"
            // The JS builds: atrule.name + " " + csstree.generate(atrule.prelude)
            // which gives "media screen" or "media only screen and (...)"
            // We need to minify whitespace around colons and parentheses to match
            // the format expected by useMqs params.
            val raw = prelude.removePrefix("@").trim()
            // Collapse whitespace and remove spaces around colons (like csstree.generate)
            return raw
                .replace(Regex("\\s+"), " ")
                .replace(Regex("\\s*:\\s*"), ":")
        }

        @Suppress("LongParameterList")
        private fun parseInnerRules(
            innerCss: String,
            mediaQuery: String,
            useMqs: List<String>,
            usePseudos: List<String>,
            selectors: MutableList<CssSelector>,
            skippedRules: MutableList<SkippedRule>,
        ) {
            var i = 0
            while (i < innerCss.length) {
                while (i < innerCss.length && innerCss[i].isWhitespace()) i++
                if (i >= innerCss.length) break

                val braceIdx = innerCss.indexOf('{', startIndex = i)
                if (braceIdx < 0) break

                val selectorText = innerCss.substring(startIndex = i, endIndex = braceIdx).trim()
                val closeIdx = innerCss.indexOf('}', startIndex = braceIdx + 1)
                if (closeIdx < 0) break

                val declText = innerCss.substring(startIndex = braceIdx + 1, endIndex = closeIdx).trim()
                if (selectorText.isNotEmpty() && declText.isNotEmpty()) {
                    processSelectorList(
                        selectorText = selectorText,
                        declarationsText = declText,
                        mediaQuery = mediaQuery,
                        useMqs = useMqs,
                        usePseudos = usePseudos,
                        selectors = selectors,
                        skippedRules = skippedRules,
                    )
                }

                i = closeIdx + 1
            }
        }

        /**
         * Processes a comma-separated selector list, handling pseudo-classes.
         * Selectors that pass the pseudo filter are added to [selectors] for inlining.
         * Selectors that are skipped (pseudo not in usePseudos) are added to
         * [skippedRules] so they can be preserved in the rebuilt stylesheet.
         */
        @Suppress("LongParameterList")
        private fun processSelectorList(
            selectorText: String,
            declarationsText: String,
            mediaQuery: String,
            useMqs: List<String>,
            usePseudos: List<String>,
            selectors: MutableList<CssSelector>,
            skippedRules: MutableList<SkippedRule>,
        ) {
            if (!useMqs.contains(mediaQuery)) return

            val declarations = parseDeclarations(declarationsText)

            // Handle comma-separated selectors
            val selectorParts = selectorText.split(',')
            val skippedParts = mutableListOf<String>()

            selectorParts.forEach { sel ->
                val trimmedSel = sel.trim()
                if (trimmedSel.isEmpty()) return@forEach

                val wasProcessed = processSingleSelector(
                    selector = trimmedSel,
                    declarations = declarations,
                    usePseudos = usePseudos,
                    selectors = selectors,
                    fullSelectorGroup = selectorText.trim(),
                )
                if (!wasProcessed) {
                    skippedParts.add(trimmedSel)
                }
            }

            // If some selectors in this rule were skipped, preserve them
            if (skippedParts.isNotEmpty()) {
                skippedRules.add(
                    SkippedRule(
                        selectorText = skippedParts.joinToString(separator = ","),
                        declarationsText = declarationsText,
                    ),
                )
            }
        }

        /**
         * Returns true if the selector was processed (added to selectors list),
         * false if it was skipped.
         */
        private fun processSingleSelector(
            selector: String,
            declarations: List<Declaration>,
            usePseudos: List<String>,
            selectors: MutableList<CssSelector>,
            fullSelectorGroup: String,
        ): Boolean {
            // Extract pseudo-classes/elements from selector
            val pseudoParts = extractPseudoParts(selector)
            val pseudoSelector = pseudoParts.joinToString(separator = "") { it }

            if (!usePseudos.contains(pseudoSelector)) {
                return false
            }

            // Remove pseudo parts from selector for matching
            val matchSelector = removePseudoParts(selector, pseudoParts)
            val specificity = calculateSpecificity(selector)
            val classNames = extractClassNames(selector)
            val idName = extractIdName(selector)

            selectors.add(
                CssSelector(
                    originalSelector = selector,
                    matchSelector = matchSelector.ifEmpty { selector },
                    declarations = declarations,
                    specificity = specificity,
                    classNames = classNames,
                    idName = idName,
                    fullSelectorGroup = fullSelectorGroup,
                ),
            )
            return true
        }

        /**
         * Extract non-preserved pseudo-class/element selectors.
         */
        private fun extractPseudoParts(selector: String): List<String> {
            val pseudos = mutableListOf<String>()
            val regex = Regex("(::?[a-zA-Z][a-zA-Z0-9-]*(?:\\([^)]*\\))?)")
            for (match in regex.findAll(selector)) {
                val pseudoName = match.value
                    .removePrefix("::")
                    .removePrefix(":")
                    .substringBefore('(')
                if (pseudoName !in PRESERVED_PSEUDOS) {
                    pseudos.add(match.value)
                }
            }
            return pseudos
        }

        private fun removePseudoParts(selector: String, parts: List<String>): String {
            var result = selector
            for (part in parts) {
                result = result.replace(part, "")
            }
            return result.trim()
        }

        /**
         * Calculates CSS specificity for a selector.
         * Returns (a, b, c) where:
         * - a = ID selectors
         * - b = class selectors, attribute selectors, pseudo-classes
         * - c = type selectors, pseudo-elements
         */
        private fun calculateSpecificity(selector: String): Specificity {
            var a = 0
            var b = 0
            var c = 0

            // Remove :not() content but count its inner selectors
            val withoutNot = selector.replace(Regex(":not\\(([^)]*)\\)")) { match ->
                val inner = match.groupValues[1]
                val innerSpec = calculateSpecificity(inner)
                a += innerSpec.a
                b += innerSpec.b
                c += innerSpec.c
                ""
            }

            // Count ID selectors (#id)
            a += Regex("#[a-zA-Z_][a-zA-Z0-9_-]*").findAll(withoutNot).count()

            // Count class selectors (.class)
            b += Regex("\\.[a-zA-Z_][a-zA-Z0-9_-]*").findAll(withoutNot).count()

            // Count attribute selectors ([attr...])
            b += Regex("\\[[^]]+]").findAll(withoutNot).count()

            // Count pseudo-classes (single :, not ::)
            b += Regex(":(?!:)[a-zA-Z][a-zA-Z0-9-]*").findAll(withoutNot).count()

            // Count pseudo-elements (::)
            c += Regex("::[a-zA-Z][a-zA-Z0-9-]*").findAll(withoutNot).count()

            // Count type selectors (element names)
            // Remove all IDs, classes, attrs, pseudos first, then count remaining names
            val stripped = withoutNot
                .replace(Regex("#[a-zA-Z_][a-zA-Z0-9_-]*"), "")
                .replace(Regex("\\.[a-zA-Z_][a-zA-Z0-9_-]*"), "")
                .replace(Regex("\\[[^]]+]"), "")
                .replace(Regex("::?[a-zA-Z][a-zA-Z0-9-]*(?:\\([^)]*\\))?"), "")
            c += Regex("(?:^|[\\s>+~])([a-zA-Z][a-zA-Z0-9]*)").findAll(stripped).count()

            return Specificity(a = a, b = b, c = c)
        }

        private fun extractClassNames(selector: String): List<String> =
            Regex("\\.([-a-zA-Z_][a-zA-Z0-9_-]*)").findAll(selector)
                .map { it.groupValues[1] }
                .toList()

        private fun extractIdName(selector: String): String? =
            Regex("#([-a-zA-Z_][a-zA-Z0-9_-]*)").find(selector)?.groupValues?.get(1)

        private fun parseDeclarations(text: String): List<Declaration> {
            val result = mutableListOf<Declaration>()
            text.split(';').forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) {
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0) {
                        val prop = trimmed.substring(startIndex = 0, endIndex = colonIdx).trim()
                        var value = trimmed.substring(startIndex = colonIdx + 1).trim()
                        val important = value.contains("!important")
                        if (important) {
                            value = value.replace("!important", "").trim()
                        }
                        result.add(
                            Declaration(
                                property = prop.lowercase(),
                                value = value,
                                important = important,
                            ),
                        )
                    }
                }
            }
            return result
        }

        /**
         * Rebuilds the stylesheet from remaining (non-removed) selectors,
         * skipped rules (pseudo not in usePseudos), and at-rules.
         */
        private fun rebuildStylesheet(entry: StyleEntry): String {
            val sb = StringBuilder()

            // Emit at-rules first
            for (atRule in entry.atRules) {
                sb.append(minifyAtRule(atRule))
            }

            // Emit remaining (non-removed) selectors grouped by declarations
            val remainingSelectors = entry.selectors.filter { !it.removed }
            val rulesByDecl = mutableMapOf<String, MutableList<CssSelector>>()
            for (sel in remainingSelectors) {
                val declKey = sel.declarations.joinToString(separator = ";") { decl ->
                    val imp = if (decl.important) "!important" else ""
                    "${decl.property}:${decl.value}$imp"
                }
                rulesByDecl.getOrPut(declKey) { mutableListOf() }.add(sel)
            }

            for ((declKey, sels) in rulesByDecl) {
                val selectorText = sels.joinToString(separator = ",") { it.originalSelector }
                sb.append(selectorText)
                sb.append('{')
                sb.append(declKey)
                sb.append('}')
            }

            // Emit skipped rules (selectors with pseudos not in usePseudos)
            for (skipped in entry.skippedRules) {
                val minifiedSelector = minifySelector(skipped.selectorText)
                // Minify as a full rule to properly remove trailing semicolons before }
                val minifiedRule = MinifyStyles.minifyCss(
                    "$minifiedSelector{${skipped.declarationsText}}"
                )
                sb.append(minifiedRule)
            }

            return sb.toString()
        }

        /**
         * Minifies a CSS selector by collapsing whitespace and removing spaces
         * around combinators (+, >, ~).
         */
        private fun minifySelector(selector: String): String =
            selector.trim()
                .replace(Regex("\\s+"), " ")
                .replace(Regex("\\s*>\\s*"), ">")
                .replace(Regex("\\s*\\+\\s*"), "+")
                .replace(Regex("\\s*~\\s*"), "~")

        private fun minifyAtRule(atRule: AtRule): String {
            if (atRule.inner == null) {
                return MinifyStyles.minifyCss(atRule.text)
            }
            val minifiedPrelude = MinifyStyles.minifyCss(atRule.text)
            val minifiedInner = MinifyStyles.minifyCss(atRule.inner)
            return "$minifiedPrelude{$minifiedInner}"
        }
    }

    private data class ResolvedParams(
        val onlyMatchedOnce: Boolean,
        val removeMatchedSelectors: Boolean,
        val useMqs: List<String>,
        val usePseudos: List<String>,
    )

    internal data class Specificity(
        val a: Int,
        val b: Int,
        val c: Int,
    )

    internal data class Declaration(
        val property: String,
        val value: String,
        val important: Boolean,
    )

    internal data class CssSelector(
        val originalSelector: String,
        val matchSelector: String,
        val declarations: List<Declaration>,
        val specificity: Specificity,
        val classNames: List<String>,
        val idName: String?,
        val fullSelectorGroup: String,
        var removed: Boolean = false,
        var matchedElements: List<XastElement>? = null,
    )

    private data class SelectorWithEntry(
        val selector: CssSelector,
        val entry: StyleEntry,
    )

    internal data class AtRule(
        val text: String,
        val inner: String? = null,
        val isMedia: Boolean = false,
    )

    internal data class SkippedRule(
        val selectorText: String,
        val declarationsText: String,
    )

    internal data class ParseResult(
        val selectors: MutableList<CssSelector>,
        val atRules: MutableList<AtRule>,
        val skippedRules: MutableList<SkippedRule>,
    )

    private data class StyleEntry(
        val node: XastElement,
        val parentNode: svgokt.domain.XastParent?,
        val selectors: MutableList<CssSelector>,
        val atRules: MutableList<AtRule>,
        val skippedRules: MutableList<SkippedRule>,
        val originalCss: String,
    )
}
