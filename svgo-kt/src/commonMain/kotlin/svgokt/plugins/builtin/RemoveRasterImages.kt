package svgokt.plugins.builtin

import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

private val rasterImagePattern = Regex("""(\.|image/)(jpe?g|png|gif)""")

/**
 * Remove raster image references in `<image>` elements.
 *
 * @see <a href="https://bugs.webkit.org/show_bug.cgi?id=63548">WebKit Bug 63548</a>
 */
object RemoveRasterImages : Plugin<NoPluginParam> {
    override val name: String = "removeRasterImages"
    override val description: String = "removes raster images"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "image") {
                        val href = node.attributes["xlink:href"]
                            ?: node.attributes["href"]
                        if (href != null && rasterImagePattern.containsMatchIn(href)) {
                            parentNode?.let { node.detachFromParent(it) }
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }
}
