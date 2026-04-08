package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Simplified plugin that removes attributes with default values.
 *
 * Removes attributes when they match their SVG spec default value,
 * since they have no visual effect. Elements with an `id` attribute
 * are skipped because they may be referenced and overriding defaults
 * intentionally.
 *
 * Default values removed:
 * - fill-rule="nonzero"
 * - clip-rule="nonzero"
 * - stroke-linecap="butt"
 * - stroke-linejoin="miter"
 * - stroke-dashoffset="0"
 * - stroke-opacity="1"
 * - fill-opacity="1"
 * - opacity="1"
 * - stroke-width="1"
 */
object RemoveUnknownsAndDefaults : Plugin<NoPluginParam> {
    override val name: String = "removeUnknownsAndDefaults"
    override val description: String =
        "removes attrs with default values"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = ::onEnter,
            ),
        )
    }

    private val defaultValues: Map<String, String> = mapOf(
        "fill-rule" to "nonzero",
        "clip-rule" to "nonzero",
        "stroke-linecap" to "butt",
        "stroke-linejoin" to "miter",
        "stroke-dashoffset" to "0",
        "stroke-opacity" to "1",
        "fill-opacity" to "1",
        "opacity" to "1",
        "stroke-width" to "1",
    )

    @Suppress("UnusedParameter")
    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
    ): VisitState {
        if (node.name.contains(":")) return VisitState.Continue
        if (node.attributes.containsKey("id")) return VisitState.Continue

        val keysToRemove = mutableListOf<String>()
        for ((attrName, attrValue) in node.attributes) {
            val defaultValue = defaultValues[attrName]
            if (defaultValue != null && defaultValue == attrValue) {
                keysToRemove.add(attrName)
            }
        }
        for (key in keysToRemove) {
            node.attributes.remove(key)
        }
        return VisitState.Continue
    }
}
