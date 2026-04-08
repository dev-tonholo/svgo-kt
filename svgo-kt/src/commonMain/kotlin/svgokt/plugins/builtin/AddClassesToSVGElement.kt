package svgokt.plugins.builtin

import svgokt.domain.XastElementType
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Adds class names to the outer `<svg>` element.
 *
 * Accepts either a single [className] or a list of [classNames].
 * Existing classes on the element are preserved; duplicates are avoided
 * by collecting everything into a [Set].
 */
object AddClassesToSVGElement : Plugin<AddClassesToSVGElement.Params> {
    data class Params(
        val className: String? = null,
        val classNames: List<String> = emptyList(),
    ) : PluginParams,
        Map<String, Any> by toMap(className, classNames) {
        companion object {
            private fun toMap(
                className: String?,
                classNames: List<String>,
            ): Map<String, Any> = buildMap {
                className?.let { put("className", it) }
                if (classNames.isNotEmpty()) put("classNames", classNames)
            }
        }
    }

    override val name: String = "addClassesToSVGElement"
    override val description: String = "adds classnames to an outer <svg> element"
    override val params: Params = Params()
    override val fn: PluginFn = { _, pluginParams, _ ->
        val resolved = resolveParams(pluginParams)
        val names = resolved.classNames.ifEmpty {
            listOfNotNull(resolved.className)
        }
        if (names.isEmpty()) {
            null
        } else {
            Visitor(
                element = VisitorNode(
                    onEnter = { node, parentNode ->
                        if (node.name == "svg" && parentNode?.type == XastElementType.ROOT) {
                            val existing = node.attributes["class"]
                            val classList = LinkedHashSet<String>()
                            if (existing != null) {
                                classList.addAll(existing.split(" ").filter { it.isNotEmpty() })
                            }
                            classList.addAll(names)
                            node.attributes["class"] = classList.joinToString(separator = " ")
                        }
                        VisitState.Continue
                    },
                ),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveParams(pluginParams: PluginParams): Params {
        if (pluginParams is Params) return pluginParams
        return Params(
            className = pluginParams["className"] as? String,
            classNames = (pluginParams["classNames"] as? List<String>).orEmpty(),
        )
    }
}
