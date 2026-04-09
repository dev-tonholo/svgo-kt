package svgokt.plugins.builtin

import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Remove width/height attributes and add the viewBox attribute if it's missing
 *
 * @example
 * <svg width="100" height="50" />
 *   ↓
 * <svg viewBox="0 0 100 50" />
 *
 * @author Benny Schudel / parsed to Kotlin by Rafael Tonholo
 */
/**
 * Formats a number the same way JavaScript's Number.toString() would:
 * integer values render without a decimal point, floats keep their fractional part.
 */
private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

val RemoveDimensions = plugin<NoPluginParam> {
    name = "removeDimensions"
    description =
        "removes width and height in presence of viewBox (opposite to removeViewBox, disable it first)"
    fn { _, _, _ ->
        val onEnterSymbol = VisitState.Continue
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    if (node.name == "svg") {
                        val widthKey = "width"
                        val heightKey = "height"
                        val viewBoxKey = "viewBox"
                        with(node.attributes) {
                            when {
                                containsKey(viewBoxKey) -> {
                                    remove(widthKey)
                                    remove(heightKey)
                                }

                                containsKey(widthKey) && containsKey(heightKey) -> {
                                    val widthStr = get(widthKey) ?: return@VisitorNode onEnterSymbol
                                    val heightStr = get(heightKey) ?: return@VisitorNode onEnterSymbol
                                    val width = widthStr.toDoubleOrNull() ?: return@VisitorNode onEnterSymbol
                                    val height = heightStr.toDoubleOrNull() ?: return@VisitorNode onEnterSymbol
                                    val widthFormatted = formatNumber(width)
                                    val heightFormatted = formatNumber(height)
                                    put(viewBoxKey, "0 0 $widthFormatted $heightFormatted")
                                    remove(widthKey)
                                    remove(heightKey)
                                }

                                else -> onEnterSymbol
                            }
                        }
                    }
                    onEnterSymbol
                }
            )
        )
    }
}
