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

/**
 * Minifies CSS in <style> elements by removing unnecessary whitespace,
 * comments, and unused rules based on document usage data.
 *
 * Follows the same architecture as the JS reference:
 * - Collects style elements, elements with style attributes, and usage data
 *   during element traversal
 * - Processes everything in root.exit
 * - Supports usage-based dead style removal
 * - Handles script/event handler deoptimization
 */
class MinifyStyles(
    override val params: Params = Params(),
) : Plugin<MinifyStyles.Params> {

    data class Params(
        val usage: UsageConfig = UsageConfig.Enabled(),
    ) : PluginParams,
        Map<String, Any> by buildParamsMap(usage)

    sealed interface UsageConfig {
        /** All usage tracking disabled. */
        data object Disabled : UsageConfig

        /** Usage tracking enabled with per-category control. */
        data class Enabled(
            val force: Boolean = false,
            val tags: Boolean = true,
            val ids: Boolean = true,
            val classes: Boolean = true,
        ) : UsageConfig
    }

    override val name: String = "minifyStyles"
    override val description: String = "minifies styles and removes unused styles"

    override val fn: PluginFn = { _, params, _ ->
        val usageCfg = resolveUsageConfig(params)

        val styleElements = mutableMapOf<XastElement, XastParent?>()
        val foreignObjectStyles = mutableSetOf<XastElement>()
        val elementsWithStyleAttrs = mutableListOf<XastElement>()
        val tagsUsage = mutableSetOf<String>()
        val idsUsage = mutableSetOf<String>()
        val classesUsage = mutableSetOf<String>()
        var deoptimized = false
        var foreignObjectDepth = 0

        val enableTagsUsage: Boolean
        val enableIdsUsage: Boolean
        val enableClassesUsage: Boolean
        val forceUsageDeoptimized: Boolean

        when (usageCfg) {
            is UsageConfig.Disabled -> {
                enableTagsUsage = false
                enableIdsUsage = false
                enableClassesUsage = false
                forceUsageDeoptimized = false
            }
            is UsageConfig.Enabled -> {
                enableTagsUsage = usageCfg.tags
                enableIdsUsage = usageCfg.ids
                enableClassesUsage = usageCfg.classes
                forceUsageDeoptimized = usageCfg.force
            }
        }

        Visitor(
            element = VisitorNode(
                onEnter = onEnter@{ node, parentNode ->
                    // Track foreignObject depth
                    if (node.name == "foreignObject") {
                        foreignObjectDepth++
                    }

                    // detect deoptimizations (scripts/event handlers)
                    if (hasScripts(node)) {
                        deoptimized = true
                    }

                    // collect tags, ids and classes usage
                    tagsUsage.add(node.name)
                    node.attributes["id"]?.let { idsUsage.add(it) }
                    node.attributes["class"]?.let { classAttr ->
                        classAttr.split(WHITESPACE_REGEX).forEach { className ->
                            if (className.isNotEmpty()) {
                                classesUsage.add(className)
                            }
                        }
                    }

                    // collect style elements or elements with style attribute
                    if (node.name == "style" && node.children.isNotEmpty()) {
                        styleElements[node] = parentNode
                        if (foreignObjectDepth > 0) {
                            foreignObjectStyles.add(node)
                        }
                    } else if (node.attributes["style"] != null) {
                        elementsWithStyleAttrs.add(node)
                    }

                    VisitState.Continue
                },
                onExit = { node, _ ->
                    if (node.name == "foreignObject") {
                        foreignObjectDepth--
                    }
                },
            ),
            root = VisitorRoot(
                onExit = { _, _ ->
                    // Build usage data for dead-code removal
                    val usageTags = if (!deoptimized || forceUsageDeoptimized) {
                        if (enableTagsUsage) tagsUsage else null
                    } else {
                        null
                    }
                    val usageIds = if (!deoptimized || forceUsageDeoptimized) {
                        if (enableIdsUsage) idsUsage else null
                    } else {
                        null
                    }
                    val usageClasses = if (!deoptimized || forceUsageDeoptimized) {
                        if (enableClassesUsage) classesUsage else null
                    } else {
                        null
                    }

                    // minify style elements
                    for ((styleNode, styleNodeParent) in styleElements) {
                        val firstChild = styleNode.children.firstOrNull()
                        if (firstChild !is XastText && firstChild !is XastCdata) continue

                        val cssText = when (firstChild) {
                            is XastText -> firstChild.value
                            is XastCdata -> firstChild.value
                            else -> continue
                        }

                        // Skip usage-based removal for styles inside foreignObject
                        val isForeignObject = foreignObjectStyles.contains(styleNode)
                        val minified = minifyCssWithUsage(
                            css = cssText,
                            usageTags = if (isForeignObject) null else usageTags,
                            usageIds = if (isForeignObject) null else usageIds,
                            usageClasses = if (isForeignObject) null else usageClasses,
                        )

                        if (minified.isEmpty()) {
                            styleNodeParent?.let { styleNode.detachFromParent(it) }
                            continue
                        }

                        // preserve cdata if necessary
                        val preserveCdata = cssText.contains('>') || cssText.contains('<')
                        val newChild = if (preserveCdata) {
                            XastCdata(value = minified)
                        } else {
                            XastText(value = minified)
                        }

                        styleNode.children.clear()
                        styleNode.children.add(newChild)
                    }

                    // minify style attributes
                    for (node in elementsWithStyleAttrs) {
                        val style = node.attributes["style"] ?: continue
                        node.attributes["style"] = minifyStyleAttribute(style)
                    }
                },
            ),
        )
    }

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val COMMENT_REGEX = Regex("/\\*[\\s\\S]*?\\*/")

        private val SCRIPT_EVENT_ATTRS = setOf(
            "onbegin", "onend", "onrepeat", "onload", "onabort", "onerror",
            "onresize", "onscroll", "onunload", "onzoom", "onclick",
            "onactivate", "onfocusin", "onfocusout", "onmousedown",
            "onmouseup", "onmouseover", "onmousemove", "onmouseout",
        )

        internal fun hasScripts(node: XastElement): Boolean {
            if (node.name == "script") return true
            return SCRIPT_EVENT_ATTRS.any { node.attributes.containsKey(it) }
        }

        /**
         * Minifies CSS with optional usage-based dead code removal.
         */
        internal fun minifyCssWithUsage(
            css: String,
            usageTags: Set<String>?,
            usageIds: Set<String>?,
            usageClasses: Set<String>?,
        ): String {
            val minified = minifyCss(css)
            if (usageTags == null && usageIds == null && usageClasses == null) {
                return minified
            }
            return removeUnusedRules(
                css = minified,
                usageTags = usageTags,
                usageIds = usageIds,
                usageClasses = usageClasses,
            )
        }

        /**
         * Performs basic CSS minification using regex-based transformations.
         */
        internal fun minifyCss(css: String): String {
            var result = css

            // Remove CSS comments
            result = result.replace(COMMENT_REGEX, "")

            // Collapse whitespace into single space
            result = result.replace(WHITESPACE_REGEX, " ")

            // Remove space around { } ; ,
            result = result.replace(Regex("\\s*\\{\\s*"), "{")
            result = result.replace(Regex("\\s*}\\s*"), "}")
            result = result.replace(Regex("\\s*;\\s*"), ";")
            result = result.replace(Regex("\\s*,\\s*"), ",")

            // Remove space around colons, but only for property:value, not inside values
            // Use a more targeted approach: space before colon and space after colon
            // when it follows a property name boundary
            result = result.replace(Regex("\\s*:\\s*"), ":")

            // Collapse multiple semicolons
            result = result.replace(Regex(";+"), ";")

            // Remove trailing semicolons before }
            result = result.replace(Regex(";+}"), "}")

            return result.trim()
        }

        /**
         * Minifies a style attribute value (declarations only, no selectors).
         */
        internal fun minifyStyleAttribute(style: String): String {
            var result = style.trim()
            result = result.replace(WHITESPACE_REGEX, " ")
            result = result.replace(Regex("\\s*;\\s*"), ";")
            result = result.replace(Regex("\\s*:\\s*"), ":")
            result = result.replace(Regex("\\s*,\\s*"), ",")
            result = result.replace(Regex(";+"), ";")
            result = result.trimEnd(';')
            return result.trim()
        }

        /**
         * Removes CSS rules whose selectors reference unused tags, IDs, or classes.
         *
         * A rule is removed if its selector only references tags/ids/classes that
         * are not present in the document. Rules inside @media blocks are also checked.
         */
        @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
        internal fun removeUnusedRules(
            css: String,
            usageTags: Set<String>?,
            usageIds: Set<String>?,
            usageClasses: Set<String>?,
        ): String {
            val result = StringBuilder()
            var i = 0

            while (i < css.length) {
                // Handle @media and other at-rules
                if (css[i] == '@') {
                    val braceIdx = css.indexOf('{', startIndex = i)
                    if (braceIdx < 0) {
                        result.append(css.substring(startIndex = i))
                        break
                    }
                    val atRulePrelude = css.substring(startIndex = i, endIndex = braceIdx)

                    // Find matching closing brace for the at-rule block
                    var depth = 1
                    var j = braceIdx + 1
                    while (j < css.length && depth > 0) {
                        if (css[j] == '{') depth++
                        if (css[j] == '}') depth--
                        j++
                    }
                    val innerCss = css.substring(startIndex = braceIdx + 1, endIndex = j - 1)
                    val filteredInner = removeUnusedRules(
                        css = innerCss,
                        usageTags = usageTags,
                        usageIds = usageIds,
                        usageClasses = usageClasses,
                    )
                    if (filteredInner.isNotEmpty()) {
                        result.append(atRulePrelude)
                        result.append('{')
                        result.append(filteredInner)
                        result.append('}')
                    }
                    i = j
                    continue
                }

                // Find next rule
                val braceIdx = css.indexOf('{', startIndex = i)
                if (braceIdx < 0) {
                    result.append(css.substring(startIndex = i))
                    break
                }

                val selectorText = css.substring(startIndex = i, endIndex = braceIdx)

                // Find closing brace
                val closeIdx = css.indexOf('}', startIndex = braceIdx + 1)
                if (closeIdx < 0) {
                    result.append(css.substring(startIndex = i))
                    break
                }

                val declarations = css.substring(startIndex = braceIdx + 1, endIndex = closeIdx)

                // Check if any selector in the comma-separated list is used
                val selectors = selectorText.split(',')
                val usedSelectors = selectors.filter { sel ->
                    isSelectorUsed(
                        selector = sel.trim(),
                        usageTags = usageTags,
                        usageIds = usageIds,
                        usageClasses = usageClasses,
                    )
                }

                if (usedSelectors.isNotEmpty()) {
                    result.append(usedSelectors.joinToString(separator = ","))
                    result.append('{')
                    result.append(declarations)
                    result.append('}')
                }

                i = closeIdx + 1
            }

            return result.toString()
        }

        /**
         * Checks if a CSS selector references tags/ids/classes that exist in the document.
         */
        @Suppress("ReturnCount")
        private fun isSelectorUsed(
            selector: String,
            usageTags: Set<String>?,
            usageIds: Set<String>?,
            usageClasses: Set<String>?,
        ): Boolean {
            if (selector.isEmpty()) return false

            // Check class selectors (.className)
            if (usageClasses != null) {
                val classMatches = Regex("\\.([a-zA-Z_][a-zA-Z0-9_-]*)").findAll(selector)
                for (match in classMatches) {
                    if (match.groupValues[1] !in usageClasses) return false
                }
            }

            // Check ID selectors (#idName)
            if (usageIds != null) {
                val idMatches = Regex("#([a-zA-Z_][a-zA-Z0-9_-]*)").findAll(selector)
                for (match in idMatches) {
                    if (match.groupValues[1] !in usageIds) return false
                }
            }

            // Check tag selectors (bare tag names at the start or after combinators)
            if (usageTags != null) {
                val tagMatches = Regex("(?:^|[\\s>+~])([a-zA-Z][a-zA-Z0-9]*)").findAll(selector)
                for (match in tagMatches) {
                    if (match.groupValues[1] !in usageTags) return false
                }
            }

            return true
        }

        /**
         * Resolves usage config from the PluginParams map.
         * Handles both boolean and object forms of the "usage" parameter.
         */
        @Suppress("ReturnCount")
        private fun resolveUsageConfig(params: PluginParams): UsageConfig {
            val usage = params["usage"] ?: return UsageConfig.Enabled()

            if (usage is Boolean) {
                return if (usage) UsageConfig.Enabled() else UsageConfig.Disabled
            }

            if (usage is Map<*, *>) {
                val force = usage["force"] as? Boolean ?: false
                val tags = usage["tags"] as? Boolean ?: true
                val ids = usage["ids"] as? Boolean ?: true
                val classes = usage["classes"] as? Boolean ?: true
                return UsageConfig.Enabled(
                    force = force,
                    tags = tags,
                    ids = ids,
                    classes = classes,
                )
            }

            if (usage is UsageConfig) return usage

            return UsageConfig.Enabled()
        }
    }
}

private fun buildParamsMap(usage: MinifyStyles.UsageConfig): Map<String, Any> = buildMap {
    put("usage", usage)
}
