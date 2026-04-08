package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.visit

// Simplified version - removes enable-background when no <filter> elements exist.
// Full implementation would parse the enable-background value and correlate it with
// filter regions, only removing when the attribute value is not referenced by any filter.

private const val ATTR_ENABLE_BACKGROUND = "enable-background"
private const val ELEMENT_FILTER = "filter"

object CleanupEnableBackground : Plugin<NoPluginParam> {
    override val name: String = "cleanupEnableBackground"
    override val description: String = "removes enable-background attribute when no filter elements are present"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = fn@{ root, _, _ ->
        // First pass: check if any <filter> elements exist in the document
        var hasFilter = false
        root.visit(
            Visitor(
                element = VisitorNode(
                    onEnter = { node, _ ->
                        if (node.name == ELEMENT_FILTER) {
                            hasFilter = true
                        }
                        VisitState.Continue
                    },
                ),
            ),
        )

        // When filters are present the relationship between enable-background and filter
        // regions is complex - defer to a full implementation for that case.
        if (hasFilter) return@fn null

        // No filters - safe to remove all enable-background attributes
        Visitor(
            element = VisitorNode(
                onEnter = CleanupEnableBackground::removeEnableBackground,
            ),
        )
    }

    private fun removeEnableBackground(node: XastElement, @Suppress("UNUSED_PARAMETER") parentNode: XastParent?): VisitState {
        node.attributes.remove(ATTR_ENABLE_BACKGROUND)
        return VisitState.Continue
    }
}
