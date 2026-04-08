package svgokt.plugins.builtin

import svgokt.domain.XastElementType
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

/**
 * Removes `<path>` elements whose move-to commands lie entirely outside
 * the SVG viewBox.
 *
 * This is a simplified implementation that checks only the `M` (absolute
 * move-to) commands in the path data. If none of the M-command coordinates
 * fall within the viewBox, the path is removed.
 *
 * Elements with a `transform` attribute are skipped because the actual
 * rendered position may differ from the raw path coordinates.
 */
object RemoveOffCanvasPaths : Plugin<NoPluginParam> {
    override val name: String = "removeOffCanvasPaths"
    override val description: String = "removes elements that are drawn outside of the viewBox"
    override val params: NoPluginParam = NoPluginParam

    private data class ViewBox(
        val left: Double,
        val top: Double,
        val right: Double,
        val bottom: Double,
    )

    private val VIEWBOX_REGEX = Regex(
        """^(-?\d*\.?\d+)\s+(-?\d*\.?\d+)\s+(\d*\.?\d+)\s+(\d*\.?\d+)$"""
    )

    private val MOVE_CMD_REGEX = Regex("""M\s*([-+]?\d*\.?\d+)[,\s]+([-+]?\d*\.?\d+)""")

    override val fn: PluginFn = { _, _, _ ->
        var viewBox: ViewBox? = null
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    // Parse viewBox from the root svg element
                    if (node.name == "svg" && parentNode?.type == XastElementType.ROOT) {
                        val vbAttr = node.attributes["viewBox"]
                            ?: buildViewBoxFromDimensions(
                                width = node.attributes["width"],
                                height = node.attributes["height"],
                            )

                        if (vbAttr != null) {
                            viewBox = parseViewBox(vbAttr)
                        }
                    }

                    // Skip elements with transforms
                    if (node.attributes.containsKey("transform")) {
                        return@VisitorNode VisitState.Skip
                    }

                    val vb = viewBox
                    if (node.name == "path" && node.attributes["d"] != null && vb != null) {
                        val d = node.attributes.getValue("d")
                        if (!hasVisibleMoveCommand(pathData = d, viewBox = vb)) {
                            parentNode?.let { node.detachFromParent(it) }
                        }
                    }

                    VisitState.Continue
                },
            ),
        )
    }

    private fun buildViewBoxFromDimensions(width: String?, height: String?): String? {
        if (width == null || height == null) return null
        return "0 0 $width $height"
    }

    private fun parseViewBox(raw: String): ViewBox? {
        val cleaned = raw
            .replace(Regex("[,+]|px"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val match = VIEWBOX_REGEX.find(cleaned) ?: return null
        val left = match.groupValues[1].toDouble()
        val top = match.groupValues[2].toDouble()
        val width = match.groupValues[3].toDouble()
        val height = match.groupValues[4].toDouble()
        return ViewBox(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        )
    }

    private fun hasVisibleMoveCommand(pathData: String, viewBox: ViewBox): Boolean =
        MOVE_CMD_REGEX.findAll(pathData).any { move ->
            val x = move.groupValues[1].toDoubleOrNull()
            val y = move.groupValues[2].toDoubleOrNull()
            x != null && y != null &&
                x >= viewBox.left && x <= viewBox.right &&
                y >= viewBox.top && y <= viewBox.bottom
        }
}
