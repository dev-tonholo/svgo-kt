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
 * Existing element attributes are not overridden.
 */
object ConvertStyleToAttrs : Plugin<ConvertStyleToAttrs.Params> {

    data class Params(
        val keepImportant: Boolean = false,
    ) : PluginParams,
        Map<String, Any> by mapOf("keepImportant" to keepImportant)

    override val name: String = "convertStyleToAttrs"
    override val description: String = "converts style to attributes"
    override val params: Params = Params()

    private val DECLARATION_REGEX = Regex(
        """([^:;\s]+)\s*:\s*([^;!]+?)(\s*!important)?\s*(?:;|$)"""
    )

    override val fn: PluginFn = { _, pluginParams, _ ->
        val resolved = pluginParams as? Params ?: Params()
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    val style = node.attributes["style"]
                    if (style != null) {
                        val remaining = mutableListOf<String>()
                        val newAttrs = mutableMapOf<String, String>()

                        for (match in DECLARATION_REGEX.findAll(style)) {
                            val prop = match.groupValues[1].trim().lowercase()
                            val value = match.groupValues[2].trim()
                            val isImportant = match.groupValues[3].isNotBlank()

                            if (isImportant && resolved.keepImportant) {
                                remaining.add("$prop:$value !important")
                                continue
                            }

                            if (prop in Collections.presentationAttrs &&
                                !node.attributes.containsKey(prop)
                            ) {
                                newAttrs[prop] = value
                            } else {
                                remaining.add("$prop:$value")
                            }
                        }

                        node.attributes.putAll(newAttrs)

                        if (remaining.isEmpty()) {
                            node.attributes.remove("style")
                        } else {
                            node.attributes["style"] = remaining.joinToString(separator = ";")
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
