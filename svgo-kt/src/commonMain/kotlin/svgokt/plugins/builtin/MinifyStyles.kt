package svgokt.plugins.builtin

import svgokt.domain.XastCdata
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastText
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

/**
 * Minifies CSS in <style> elements by removing unnecessary whitespace,
 * comments, and newlines.
 *
 * Simplified version that uses regex-based minification instead of
 * a full CSS optimizer like CSSO.
 */
object MinifyStyles : Plugin<NoPluginParam> {
    override val name: String = "minifyStyles"
    override val description: String = "minifies styles and removes unused styles"
    override val params: NoPluginParam = NoPluginParam

    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = MinifyStyles::onEnter,
            ),
        )
    }

    @Suppress("ReturnCount")
    private fun onEnter(node: XastElement, parentNode: XastParent?): VisitState {
        if (node.name == "foreignObject") {
            return VisitState.Skip
        }

        if (node.name != "style" || node.children.isEmpty()) {
            return VisitState.Continue
        }

        val type = node.attributes["type"]
        if (type != null && type != "" && type != "text/css") {
            return VisitState.Continue
        }

        val firstChild = node.children.firstOrNull()
        val cssText = when (firstChild) {
            is XastText -> firstChild.value
            is XastCdata -> firstChild.value
            else -> return VisitState.Continue
        }

        val minified = minifyCss(cssText)

        if (minified.isEmpty()) {
            parentNode?.let { node.detachFromParent(it) }
            return VisitState.Continue
        }

        val preserveCdata = cssText.contains('>') || cssText.contains('<')
        val newChild = if (preserveCdata) {
            XastCdata(value = minified)
        } else {
            XastText(value = minified)
        }

        node.children.clear()
        node.children.add(newChild)

        return VisitState.Continue
    }

    /**
     * Performs basic CSS minification using regex-based transformations:
     * - Removes CSS comments
     * - Collapses whitespace around braces, colons, semicolons, commas
     * - Removes trailing semicolons before closing braces
     * - Trims leading and trailing whitespace
     */
    internal fun minifyCss(css: String): String {
        var result = css

        // Remove CSS comments /* ... */
        result = result.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")

        // Collapse whitespace (newlines, tabs, multiple spaces) into single space
        result = result.replace(Regex("\\s+"), " ")

        // Remove space around { } : ; ,
        result = result.replace(Regex("\\s*\\{\\s*"), "{")
        result = result.replace(Regex("\\s*}\\s*"), "}")
        result = result.replace(Regex("\\s*;\\s*"), ";")
        result = result.replace(Regex("\\s*:\\s*"), ":")
        result = result.replace(Regex("\\s*,\\s*"), ",")

        // Remove trailing semicolons before }
        result = result.replace(Regex(";+}"), "}")

        return result.trim()
    }
}
