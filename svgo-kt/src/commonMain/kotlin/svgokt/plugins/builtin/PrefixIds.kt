package svgokt.plugins.builtin

import svgokt.domain.XastCdata
import svgokt.domain.XastComment
import svgokt.domain.XastText
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginInfo
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections

/**
 * Prefixes all IDs and their references with a configurable prefix.
 *
 * Updates:
 * - `id` attribute values
 * - `class` attribute values
 * - `href` and `xlink:href` references (`#id`)
 * - `url(#id)` references in presentation attributes and style attributes
 * - `begin`/`end` animation event references (`id.start`, `id.end`)
 * - Selectors and url() references inside `<style>` elements
 */
object PrefixIds : Plugin<PrefixIds.Params> {

    data class Params(
        val prefix: String? = null,
        val delim: String = DEFAULT_DELIM,
        val prefixIds: Boolean = true,
        val prefixClassNames: Boolean = true,
    ) : PluginParams,
        Map<String, Any> by toMap(prefix, delim, prefixIds, prefixClassNames) {
        companion object {
            private fun toMap(
                prefix: String?,
                delim: String,
                prefixIds: Boolean,
                prefixClassNames: Boolean,
            ): Map<String, Any> = buildMap {
                prefix?.let { put("prefix", it) }
                put("delim", delim)
                put("prefixIds", prefixIds)
                put("prefixClassNames", prefixClassNames)
            }
        }
    }

    private const val DEFAULT_DELIM = "__"
    private const val FALLBACK_PREFIX = "prefix"

    private val URL_REF_REGEX = Regex(
        pattern = """\burl\((['"]?)(#.+?)\1\)""",
        option = RegexOption.IGNORE_CASE,
    )

    /**
     * Matches an id selector (#name) in CSS. Captures the identifier name.
     */
    private val CSS_ID_SELECTOR_REGEX = Regex("""#([a-zA-Z_][a-zA-Z0-9_-]*)""")

    /**
     * Matches a class selector (.name) in CSS selectors. Captures the class name.
     * In selector context (not values), any dot followed by a valid identifier
     * start character is a class selector.
     */
    private val CSS_CLASS_SELECTOR_REGEX = Regex("""\.([a-zA-Z_][a-zA-Z0-9_-]*)""")

    override val name: String = "prefixIds"
    override val description: String = "prefix IDs"
    override val params: Params = Params()

    override val fn: PluginFn = { _, pluginParams, info ->
        val resolved = resolveParams(pluginParams)
        val computedPrefix = computePrefix(resolved, info)
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    val generator: (String) -> String = { id -> prefixId(computedPrefix, id) }

                    // Process <style> element CSS content
                    if (node.name == "style" && node.children.isNotEmpty()) {
                        processStyleElement(node, resolved, generator)
                        return@VisitorNode VisitState.Continue
                    }

                    // Prefix id attribute
                    if (resolved.prefixIds) {
                        val id = node.attributes["id"]
                        if (id != null && id.isNotEmpty()) {
                            node.attributes["id"] = generator(id)
                        }
                    }

                    // Prefix class attribute
                    if (resolved.prefixClassNames) {
                        val cls = node.attributes["class"]
                        if (cls != null && cls.isNotEmpty()) {
                            node.attributes["class"] = cls.split(Regex("\\s+"))
                                .joinToString(separator = " ") { generator(it) }
                        }
                    }

                    // Prefix href / xlink:href
                    for (hrefAttr in listOf("href", "xlink:href")) {
                        val href = node.attributes[hrefAttr]
                        if (href != null && href.isNotEmpty()) {
                            val prefixed = prefixReference(computedPrefix, href)
                            if (prefixed != null) {
                                node.attributes[hrefAttr] = prefixed
                            }
                        }
                    }

                    // Prefix url(#...) in reference props
                    for (refProp in Collections.referencesProps) {
                        val value = node.attributes[refProp]
                        if (value != null && value.isNotEmpty()) {
                            node.attributes[refProp] = prefixUrlReferences(value, generator)
                        }
                    }

                    // Prefix url(#...) in style attribute
                    val style = node.attributes["style"]
                    if (style != null && style.isNotEmpty()) {
                        node.attributes["style"] = prefixUrlReferences(style, generator)
                    }

                    // Prefix begin/end animation references
                    for (animAttr in listOf("begin", "end")) {
                        val value = node.attributes[animAttr]
                        if (value != null && value.isNotEmpty()) {
                            node.attributes[animAttr] = value.split(Regex("\\s*;\\s+"))
                                .joinToString(separator = "; ") { part ->
                                    if (part.endsWith(".end") || part.endsWith(".start")) {
                                        val dotIdx = part.lastIndexOf('.')
                                        val id = part.substring(startIndex = 0, endIndex = dotIdx)
                                        val postfix = part.substring(startIndex = dotIdx + 1)
                                        "${generator(id)}.$postfix"
                                    } else {
                                        part
                                    }
                                }
                        }
                    }

                    VisitState.Continue
                },
            ),
        )
    }

    /**
     * Processes CSS content within a `<style>` element, prefixing id/class
     * selectors and url() references. The CSS is minified as a side effect
     * of parsing and regenerating (matching the JS csstree.generate behavior).
     */
    private fun processStyleElement(
        node: svgokt.domain.XastElement,
        params: Params,
        generator: (String) -> String,
    ) {
        val newChildren = node.children.map { child ->
            when (child) {
                is XastText -> {
                    val processed = prefixCssText(child.value, params, generator)
                    XastText(value = processed)
                }
                is XastCdata -> {
                    val processed = prefixCssText(child.value, params, generator)
                    XastCdata(value = processed)
                }
                else -> child
            }
        }
        node.children.clear()
        node.children.addAll(newChildren)
    }

    /**
     * Prefixes id selectors, class selectors, and url() references in CSS text.
     * Also minifies the CSS (matching the JS csstree.generate output).
     */
    private fun prefixCssText(
        cssText: String,
        params: Params,
        generator: (String) -> String,
    ): String {
        val minified = minifyCss(cssText)
        return prefixCssSelectors(minified, params, generator)
    }

    /**
     * Prefixes CSS selectors and url() references within already-minified CSS.
     * Processes rule-by-rule to distinguish selectors from declaration values.
     */
    private fun prefixCssSelectors(
        css: String,
        params: Params,
        generator: (String) -> String,
    ): String {
        val result = StringBuilder()
        var i = 0

        while (i < css.length) {
            val braceIdx = css.indexOf('{', startIndex = i)
            if (braceIdx < 0) {
                result.append(css.substring(startIndex = i))
                break
            }

            // Process selector portion
            val selectorText = css.substring(startIndex = i, endIndex = braceIdx)
            result.append(prefixSelectorText(selectorText, params, generator))
            result.append('{')

            // Find closing brace
            val closeIdx = css.indexOf('}', startIndex = braceIdx + 1)
            if (closeIdx < 0) {
                result.append(css.substring(startIndex = braceIdx + 1))
                break
            }

            // Process declarations - only prefix url() references
            val declarations = css.substring(startIndex = braceIdx + 1, endIndex = closeIdx)
            result.append(prefixUrlReferences(declarations, generator))
            result.append('}')

            i = closeIdx + 1
        }

        return result.toString()
    }

    /**
     * Prefixes id and class selectors within a CSS selector string.
     */
    private fun prefixSelectorText(
        selectorText: String,
        params: Params,
        generator: (String) -> String,
    ): String {
        var result = selectorText

        if (params.prefixIds) {
            result = CSS_ID_SELECTOR_REGEX.replace(result) { match ->
                "#${generator(match.groupValues[1])}"
            }
        }

        if (params.prefixClassNames) {
            result = CSS_CLASS_SELECTOR_REGEX.replace(result) { match ->
                ".${generator(match.groupValues[1])}"
            }
        }

        return result
    }

    /**
     * Replaces url(#id) references in CSS text or attribute values.
     */
    private fun prefixUrlReferences(
        text: String,
        generator: (String) -> String,
    ): String = URL_REF_REGEX.replace(text) { match ->
        val url = match.groupValues[2]
        if (url.startsWith("#")) {
            "url(#${generator(url.substring(startIndex = 1))})"
        } else {
            match.value
        }
    }

    /**
     * Inserts a prefix before a reference string that starts with #.
     */
    private fun prefixReference(prefix: String, reference: String): String? {
        if (reference.startsWith("#")) {
            return "#" + prefixId(prefix, reference.substring(startIndex = 1))
        }
        return null
    }

    /**
     * Basic CSS minification matching csstree.generate output.
     */
    private fun minifyCss(css: String): String {
        var result = css
        // Remove CSS comments
        result = result.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        // Collapse whitespace
        result = result.replace(Regex("\\s+"), " ")
        // Remove space around { } ; ,
        result = result.replace(Regex("\\s*\\{\\s*"), "{")
        result = result.replace(Regex("\\s*\\}\\s*"), "}")
        result = result.replace(Regex("\\s*;\\s*"), ";")
        result = result.replace(Regex("\\s*,\\s*"), ",")
        result = result.replace(Regex("\\s*:\\s*"), ":")
        // Collapse multiple semicolons
        result = result.replace(Regex(";+"), ";")
        // Remove trailing semicolons before }
        result = result.replace(Regex(";+\\}"), "}")
        return result.trim()
    }

    private fun computePrefix(params: Params, info: PluginInfo): String {
        val raw = params.prefix
        if (raw != null) return raw + params.delim
        val path = info.path
        val basename = if (path != null && path.isNotEmpty()) {
            escapeIdentifierName(path.substringAfterLast('/').substringAfterLast('\\'))
        } else {
            FALLBACK_PREFIX
        }
        return basename + params.delim
    }

    private fun escapeIdentifierName(str: String): String =
        str.replace(Regex("[. ]"), "_")

    private fun prefixId(prefix: String, body: String): String =
        if (body.startsWith(prefix)) body else prefix + body

    private fun resolveParams(pluginParams: PluginParams): Params {
        if (pluginParams is Params) return pluginParams
        return Params(
            prefix = pluginParams["prefix"] as? String,
            delim = (pluginParams["delim"] as? String) ?: DEFAULT_DELIM,
            prefixIds = (pluginParams["prefixIds"] as? Boolean) ?: true,
            prefixClassNames = (pluginParams["prefixClassNames"] as? Boolean) ?: true,
        )
    }
}
