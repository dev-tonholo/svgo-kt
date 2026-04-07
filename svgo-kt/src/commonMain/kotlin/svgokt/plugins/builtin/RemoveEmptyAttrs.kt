package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

private val conditionalProcessingAttrs = setOf(
    "requiredExtensions",
    "requiredFeatures",
    "systemLanguage",
)

object RemoveEmptyAttrs : Plugin<NoPluginParam> {
    override val name: String = "removeEmptyAttrs"
    override val description: String = "remove empty attributes"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = RemoveEmptyAttrs::onEnter,
            ),
        )
    }

    private fun onEnter(node: XastElement, @Suppress("UNUSED_PARAMETER") parentNode: XastParent?): VisitState {
        node.attributes.entries.removeAll { (name, value) ->
            value.isEmpty() && name !in conditionalProcessingAttrs
        }
        return VisitState.Continue
    }
}
