package svgokt.plugins.builtin

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
 * - `url(#id)` references in presentation attributes
 * - `begin`/`end` animation event references (`id.start`, `id.end`)
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

    private val URL_REF_REGEX = Regex("""\burl\((['"])?(\#.+?)\1\)""", RegexOption.IGNORE_CASE)

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
                        if (href != null && href.startsWith("#")) {
                            node.attributes[hrefAttr] = "#" + generator(href.substring(startIndex = 1))
                        }
                    }

                    // Prefix url(#...) in reference props
                    for (refProp in Collections.referencesProps) {
                        val value = node.attributes[refProp]
                        if (value != null && value.contains("url(")) {
                            node.attributes[refProp] = URL_REF_REGEX.replace(value) { match ->
                                val url = match.groupValues[2]
                                if (url.startsWith("#")) {
                                    "url(${generator("#" + url.substring(startIndex = 1)).removePrefix("#")})"
                                        .let { "url(#${generator(url.substring(startIndex = 1))})" }
                                } else {
                                    match.value
                                }
                            }
                        }
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

    private fun computePrefix(params: Params, info: PluginInfo): String {
        val raw = params.prefix
        if (raw != null) {
            return raw + params.delim
        }
        val path = info.path
        if (path != null && path.isNotEmpty()) {
            val basename = path.substringAfterLast('/').substringAfterLast('\\')
            return escapeIdentifierName(basename) + params.delim
        }
        return FALLBACK_PREFIX + params.delim
    }

    private fun escapeIdentifierName(str: String): String =
        str.replace(Regex("[. ]"), "_")

    private fun prefixId(prefix: String, body: String): String =
        if (body.startsWith(prefix)) body else prefix + body
}
