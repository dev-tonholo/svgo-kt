package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.path.PathDataItem
import svgokt.path.stringifyPathData
import svgokt.plugins.xast.detachFromParent

private val REG_NUMBER = Regex("""[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?""")

/**
 * Parameters for the ConvertShapeToPath plugin.
 *
 * @property convertArcs Whether to convert circles and ellipses to path arcs.
 * @property floatPrecision Precision for floating point numbers in the output.
 */
data class ConvertShapeToPathParams(
    val convertArcs: Boolean = false,
    val floatPrecision: Int? = null,
) : PluginParams,
    Map<String, Any> by convertShapeToPathParamsMap(
        convertArcs = convertArcs,
        floatPrecision = floatPrecision,
    )

private fun convertShapeToPathParamsMap(
    convertArcs: Boolean,
    floatPrecision: Int?,
): Map<String, Any> = buildMap {
    put("convertArcs", convertArcs)
    if (floatPrecision != null) {
        put("floatPrecision", floatPrecision)
    }
}

/**
 * Converts basic SVG shapes (rect, line, polyline, polygon, circle, ellipse)
 * to equivalent path elements for further optimization.
 *
 * @see <a href="https://www.w3.org/TR/SVG11/shapes.html">SVG Shapes</a>
 */
object ConvertShapeToPath : Plugin<ConvertShapeToPathParams> {
    override val name: String = "convertShapeToPath"
    override val description: String = "converts basic shapes to more compact path form"
    override val params: ConvertShapeToPathParams = ConvertShapeToPathParams()
    override val fn: PluginFn = { _, params, _ ->
        val resolved = resolveParams(params)
        val convertArcs = resolved.convertArcs
        val precision = resolved.floatPrecision

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    onEnter(
                        node = node,
                        parentNode = parentNode,
                        convertArcs = convertArcs,
                        precision = precision,
                    )
                },
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
        convertArcs: Boolean,
        precision: Int?,
    ): VisitState {
        val result = when (node.name) {
            "rect" -> convertRect(node = node, precision = precision)
            "line" -> convertLine(node = node, precision = precision)
            "polyline", "polygon" -> convertPolylineOrPolygon(
                node = node,
                parentNode = parentNode,
                precision = precision,
            )
            "circle" -> if (convertArcs) convertCircle(node = node, precision = precision) else null
            "ellipse" -> if (convertArcs) convertEllipse(node = node, precision = precision) else null
            else -> null
        }

        if (result != null) {
            replaceInParent(original = node, replacement = result, parentNode = parentNode)
        }

        return VisitState.Continue
    }

    private fun replaceInParent(
        original: XastElement,
        replacement: XastElement,
        parentNode: XastParent?,
    ) {
        parentNode?.children?.let { siblings ->
            val index = siblings.indexOf(original)
            if (index >= 0) {
                siblings[index] = replacement
            }
        }
    }

    @Suppress("ReturnCount")
    private fun convertRect(node: XastElement, precision: Int?): XastElement? {
        if (node.attributes["width"] == null || node.attributes["height"] == null) return null
        if (node.attributes["rx"] != null || node.attributes["ry"] != null) return null

        val x = (node.attributes["x"] ?: "0").toDoubleOrNull() ?: return null
        val y = (node.attributes["y"] ?: "0").toDoubleOrNull() ?: return null
        val width = node.attributes["width"]?.toDoubleOrNull() ?: return null
        val height = node.attributes["height"]?.toDoubleOrNull() ?: return null

        if ((x - y + width - height).isNaN()) return null

        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(x, y)),
            PathDataItem(command = 'H', args = mutableListOf(x + width)),
            PathDataItem(command = 'V', args = mutableListOf(y + height)),
            PathDataItem(command = 'H', args = mutableListOf(x)),
            PathDataItem(command = 'z', args = mutableListOf()),
        )

        val newAttrs = node.attributes.toMutableMap().apply {
            remove("x")
            remove("y")
            remove("width")
            remove("height")
        }
        newAttrs["d"] = stringifyPathData(pathData = pathData, precision = precision)

        return XastElement(
            name = "path",
            attributes = newAttrs,
            children = node.children,
        )
    }

    @Suppress("ReturnCount")
    private fun convertLine(node: XastElement, precision: Int?): XastElement? {
        val x1 = (node.attributes["x1"] ?: "0").toDoubleOrNull() ?: return null
        val y1 = (node.attributes["y1"] ?: "0").toDoubleOrNull() ?: return null
        val x2 = (node.attributes["x2"] ?: "0").toDoubleOrNull() ?: return null
        val y2 = (node.attributes["y2"] ?: "0").toDoubleOrNull() ?: return null

        if ((x1 - y1 + x2 - y2).isNaN()) return null

        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(x1, y1)),
            PathDataItem(command = 'L', args = mutableListOf(x2, y2)),
        )

        val newAttrs = node.attributes.toMutableMap().apply {
            remove("x1")
            remove("y1")
            remove("x2")
            remove("y2")
        }
        newAttrs["d"] = stringifyPathData(pathData = pathData, precision = precision)

        return XastElement(
            name = "path",
            attributes = newAttrs,
            children = node.children,
        )
    }

    @Suppress("ReturnCount")
    private fun convertPolylineOrPolygon(
        node: XastElement,
        parentNode: XastParent?,
        precision: Int?,
    ): XastElement? {
        val pointsAttr = node.attributes["points"] ?: return null

        val coords = REG_NUMBER.findAll(pointsAttr).map { it.value.toDouble() }.toList()
        if (coords.size < MINIMUM_POLY_COORDS) {
            parentNode?.let { node.detachFromParent(it) }
            return null
        }

        val pathData = mutableListOf<PathDataItem>()
        for (i in coords.indices step 2) {
            if (i + 1 >= coords.size) break
            pathData.add(
                PathDataItem(
                    command = if (i == 0) 'M' else 'L',
                    args = mutableListOf(coords[i], coords[i + 1]),
                ),
            )
        }
        if (node.name == "polygon") {
            pathData.add(PathDataItem(command = 'z', args = mutableListOf()))
        }

        val newAttrs = node.attributes.toMutableMap().apply {
            remove("points")
        }
        newAttrs["d"] = stringifyPathData(pathData = pathData, precision = precision)

        return XastElement(
            name = "path",
            attributes = newAttrs,
            children = node.children,
        )
    }

    @Suppress("ReturnCount")
    private fun convertCircle(node: XastElement, precision: Int?): XastElement? {
        val cx = (node.attributes["cx"] ?: "0").toDoubleOrNull() ?: return null
        val cy = (node.attributes["cy"] ?: "0").toDoubleOrNull() ?: return null
        val r = (node.attributes["r"] ?: "0").toDoubleOrNull() ?: return null

        if ((cx - cy + r).isNaN()) return null

        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(cx, cy - r)),
            PathDataItem(command = 'A', args = mutableListOf(r, r, 0.0, 1.0, 0.0, cx, cy + r)),
            PathDataItem(command = 'A', args = mutableListOf(r, r, 0.0, 1.0, 0.0, cx, cy - r)),
            PathDataItem(command = 'z', args = mutableListOf()),
        )

        val newAttrs = node.attributes.toMutableMap().apply {
            remove("cx")
            remove("cy")
            remove("r")
        }
        newAttrs["d"] = stringifyPathData(pathData = pathData, precision = precision)

        return XastElement(
            name = "path",
            attributes = newAttrs,
            children = node.children,
        )
    }

    @Suppress("ReturnCount")
    private fun convertEllipse(node: XastElement, precision: Int?): XastElement? {
        val cx = (node.attributes["cx"] ?: "0").toDoubleOrNull() ?: return null
        val cy = (node.attributes["cy"] ?: "0").toDoubleOrNull() ?: return null
        val rx = (node.attributes["rx"] ?: "0").toDoubleOrNull() ?: return null
        val ry = (node.attributes["ry"] ?: "0").toDoubleOrNull() ?: return null

        if ((cx - cy + rx - ry).isNaN()) return null

        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(cx, cy - ry)),
            PathDataItem(command = 'A', args = mutableListOf(rx, ry, 0.0, 1.0, 0.0, cx, cy + ry)),
            PathDataItem(command = 'A', args = mutableListOf(rx, ry, 0.0, 1.0, 0.0, cx, cy - ry)),
            PathDataItem(command = 'z', args = mutableListOf()),
        )

        val newAttrs = node.attributes.toMutableMap().apply {
            remove("cx")
            remove("cy")
            remove("rx")
            remove("ry")
        }
        newAttrs["d"] = stringifyPathData(pathData = pathData, precision = precision)

        return XastElement(
            name = "path",
            attributes = newAttrs,
            children = node.children,
        )
    }

    private fun resolveParams(params: PluginParams): ConvertShapeToPathParams {
        if (params is ConvertShapeToPathParams) return params
        val convertArcs = when (val raw = params["convertArcs"]) {
            is Boolean -> raw
            else -> false
        }
        val floatPrecision = when (val raw = params["floatPrecision"]) {
            is Number -> raw.toInt()
            else -> null
        }
        return ConvertShapeToPathParams(
            convertArcs = convertArcs,
            floatPrecision = floatPrecision,
        )
    }

    private const val MINIMUM_POLY_COORDS = 4
}
