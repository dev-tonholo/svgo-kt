package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

private val newlinesBetweenNonWhitespace = Regex("""(\S)\r?\n(\S)""")
private val remainingNewlines = Regex("""\r?\n""")
private val multipleSpaces = Regex("""\s{2,}""")

object CleanupAttrs : Plugin<NoPluginParam> {
    override val name: String = "cleanupAttrs"
    override val description: String = "cleanup attributes from newlines, trailing and repeating spaces"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = CleanupAttrs::onEnter,
            ),
        )
    }

    private fun onEnter(node: XastElement, @Suppress("UNUSED_PARAMETER") parentNode: XastParent?): VisitState {
        for ((name, value) in node.attributes.entries.toList()) {
            val cleaned = value
                .replace(newlinesBetweenNonWhitespace, "$1 $2")
                .replace(remainingNewlines, "")
                .trim()
                .replace(multipleSpaces, " ")
            node.attributes[name] = cleaned
        }
        return VisitState.Continue
    }
}
