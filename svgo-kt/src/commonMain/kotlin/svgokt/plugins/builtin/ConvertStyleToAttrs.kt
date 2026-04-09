package svgokt.plugins.builtin

import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections

/**
 * Converts inline `style` attributes to individual SVG presentation attributes.
 *
 * For example:
 * ```
 * <g style="fill:#000; stroke:blue">
 *   becomes
 * <g fill="#000" stroke="blue">
 * ```
 *
 * Only known SVG presentation attributes are converted. Properties that
 * are not presentation attributes (e.g. vendor-prefixed) remain in the
 * `style` attribute. If all properties are converted, the `style`
 * attribute is removed entirely.
 *
 * Existing element attributes are not overridden. Also handles CSS
 * comment stripping, escape sequences, quoted strings, and
 * parenthesized values (like url(...)).
 */
object ConvertStyleToAttrs : Plugin<ConvertStyleToAttrs.Params> {

    data class Params(
        val keepImportant: Boolean = false,
    ) : PluginParams,
        Map<String, Any> by mapOf("keepImportant" to keepImportant)

    override val name: String = "convertStyleToAttrs"
    override val description: String = "converts style to attributes"
    override val params: Params = Params()

    // Port of the JS regex from convertStyleToAttrs.js.
    // Escape sequence: \\(?:[0-9a-f]{1,6}\s?|\r\n|.)
    private const val R_ESCAPE = """\\(?:[0-9a-f]{1,6}\s?|\r\n|.)"""

    // Group helper - non-capturing alternation
    private fun g(vararg args: String): String =
        "(?:" + args.joinToString("|") + ")"

    // Attribute name pattern
    private val rAttr = """\s*(""" + g("""[^:;\\\s]""", R_ESCAPE) + """*?)\s*"""

    // Quoted strings
    private val rSingleQuotes = """'(?:[^'\n\r\\]|""" + R_ESCAPE + """)*?(?:'|$)"""
    private val rQuotes = "\"(?:[^\"\\n\\r\\\\]|" + R_ESCAPE + ")*?(?:\"|$)"

    private val rQuotedString = Regex("^" + g(rSingleQuotes, rQuotes) + "$")

    // Parenthesized values like url(...)
    private val rParenthesis =
        """\(""" + g("""[^'"()\\]+""", R_ESCAPE, rSingleQuotes, rQuotes) + """*?\)"""

    // Value pattern
    private val rValue =
        """\s*(""" + g(
            """[^!'"();\\]+?""",
            R_ESCAPE,
            rSingleQuotes,
            rQuotes,
            rParenthesis,
            """[^;]*?""",
        ) + """*?)"""

    // End of declaration
    private const val R_DECL_END = """\s*(?:;\s*|$)"""

    // Important rule
    private const val R_IMPORTANT = """(\s*!important(?![-(\\w]))?"""

    // Final RegExp to parse CSS declarations
    private val regDeclarationBlock = Regex(
        pattern = rAttr + ":" + rValue + R_IMPORTANT + R_DECL_END,
        option = RegexOption.IGNORE_CASE,
    )

    // Comments expression. Honors escape sequences and strings.
    private val regStripComments = Regex(
        pattern = g(R_ESCAPE, rSingleQuotes, rQuotes, """/\*[\s\S]*?\*/"""),
        option = RegexOption.IGNORE_CASE,
    )

    override val fn: PluginFn = { _, pluginParams, _ ->
        val resolved = resolveParams(pluginParams)
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    val style = node.attributes["style"]
                    if (style != null) {
                        processStyleAttribute(node, style, resolved)
                    }
                    VisitState.Continue
                },
            ),
        )
    }

    private fun resolveParams(pluginParams: PluginParams): Params {
        if (pluginParams is Params) return pluginParams
        val defaults = Params()
        return Params(
            keepImportant = (pluginParams["keepImportant"] as? Boolean) ?: defaults.keepImportant,
        )
    }

    private fun processStyleAttribute(
        node: svgokt.domain.XastElement,
        style: String,
        params: Params,
    ) {
        // Strip CSS comments preserving escape sequences and strings
        val styleValue = regStripComments.replace(style) { match ->
            val m = match.value
            when {
                m.startsWith("/") -> ""
                m.startsWith("\\") && m.length > 1 && m[1].let { c ->
                    c in 'g'..'z' || c in 'G'..'Z'
                } -> m.substring(startIndex = 1)
                else -> m
            }
        }

        // Parse declarations
        val declarations = mutableListOf<Pair<String, String>>()
        var matchResult = regDeclarationBlock.find(styleValue)
        while (matchResult != null) {
            val isImportant = matchResult.groupValues[3].isNotBlank()
            if (!params.keepImportant || !isImportant) {
                declarations.add(
                    matchResult.groupValues[1] to matchResult.groupValues[2],
                )
            }
            matchResult = matchResult.next()
        }

        if (declarations.isEmpty()) return

        val newAttributes = mutableMapOf<String, String>()
        val remaining = declarations.filter { (prop, value) ->
            if (prop.isNotEmpty()) {
                val propLower = prop.lowercase()
                var resolvedValue = value

                if (rQuotedString.matches(resolvedValue)) {
                    resolvedValue = resolvedValue.substring(
                        startIndex = 1,
                        endIndex = resolvedValue.length - 1,
                    )
                }

                if (propLower in Collections.presentationAttrs &&
                    !node.attributes.containsKey(propLower)
                ) {
                    newAttributes[propLower] = resolvedValue
                    return@filter false
                }
            }
            true
        }

        node.attributes.putAll(newAttributes)

        if (remaining.isNotEmpty()) {
            node.attributes["style"] = remaining.joinToString(separator = ";") { (prop, value) ->
                "$prop:$value"
            }
        } else {
            node.attributes.remove("style")
        }
    }
}
