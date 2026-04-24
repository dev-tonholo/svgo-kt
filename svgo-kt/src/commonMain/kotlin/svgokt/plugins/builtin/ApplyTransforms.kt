package svgokt.plugins.builtin

import svgokt.Tools
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.path.PathDataItem
import svgokt.path.path2js
import svgokt.plugins.Collections
import svgokt.plugins.attrsGroupsDefaults
import svgokt.transform.multiplyTransformMatrices
import svgokt.transform.parseTransform
import svgokt.transform.transformsMultiply
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val REG_NUMERIC_VALUES = """[-+]?(\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?""".toRegex()

private const val ARC_RX_INDEX = 0
private const val ARC_RY_INDEX = 1
private const val ARC_ROTATION_INDEX = 2

@Suppress("UnusedPrivateProperty")
private const val ARC_LARGE_ARC_INDEX = 3
private const val ARC_SWEEP_INDEX = 4
private const val ARC_X_INDEX = 5
private const val ARC_Y_INDEX = 6
private const val ROTATION_THRESHOLD = 80.0
private const val DECOMPOSE_EPSILON = 1e-6

private fun toFixedNumber(value: Double, precision: Int): Double {
    val factor = 10.0.pow(precision)
    return floor(value * factor + 0.5) / factor
}

/**
 * Apply transformation(s) to the Path data.
 */
fun applyTransforms(root: XastRoot, params: ApplyTransformsParams): Visitor? {
    return Visitor(
        element = VisitorNode(
            onEnter = { node, parentNode ->
                applyTransformToElement(
                    node = node,
                    parentNode = parentNode,
                    params = params,
                )
            }
        )
    )
}

@Suppress("ReturnCount", "CyclomaticComplexity", "LongMethod")
private fun applyTransformToElement(
    node: XastElement,
    parentNode: XastParent?,
    params: ApplyTransformsParams,
): VisitState {
    if (node.attributes["d"] == null) {
        return VisitState.Continue
    }
    if (node.attributes["id"] != null) {
        return VisitState.Continue
    }
    val transform = node.attributes["transform"]
    val isUrlReference = node.attributes.any { (name, value) ->
        Collections.referencesProps.contains(name) && Tools.includesUrlReference(value)
    }
    if (
        transform.isNullOrEmpty() ||
        node.attributes["style"] != null ||
        isUrlReference
    ) {
        return VisitState.Continue
    }
    val transforms = parseTransform(transform)
    if (transforms.isEmpty()) {
        return VisitState.Continue
    }
    val matrix = transformsMultiply(transforms)
    val stroke = resolveInheritedAttribute(node, parentNode, "stroke")
    val strokeWidth = resolveInheritedAttribute(node, parentNode, "stroke-width")
    val scale = toFixedNumber(
        value = hypot(matrix.data[0], matrix.data[1]),
        precision = params.transformPrecision,
    )
    if (stroke != null && stroke != "none") {
        if (!params.applyTransformsStroked) {
            return VisitState.Continue
        }
        if (
            (matrix.data[0] != matrix.data[3] || matrix.data[1] != -matrix.data[2]) &&
            (matrix.data[0] != -matrix.data[3] || matrix.data[1] != matrix.data[2])
        ) {
            return VisitState.Continue
        }
        if (scale != 1.0) {
            if (node.attributes["vector-effect"] != "non-scaling-stroke") {
                val effectiveStrokeWidth = strokeWidth
                    ?: attrsGroupsDefaults["presentation"]?.get("stroke-width") ?: "1"
                node.attributes["stroke-width"] = scaleNumericValues(effectiveStrokeWidth.trim(), scale)
                val dashOffset = node.attributes["stroke-dashoffset"]
                if (dashOffset != null) {
                    node.attributes["stroke-dashoffset"] = scaleNumericValues(dashOffset.trim(), scale)
                }
                val dashArray = node.attributes["stroke-dasharray"]
                if (dashArray != null) {
                    node.attributes["stroke-dasharray"] = scaleNumericValues(dashArray.trim(), scale)
                }
            }
        }
    }
    val pathData = path2js(node).map { item ->
        PathDataItem(command = item.command, args = item.args.toMutableList())
    }
    applyMatrixToPathData(pathData = pathData, matrix = matrix.data)
    node.attributes["d"] = svgokt.path.stringifyPathData(
        pathData = pathData,
        precision = null,
        disableSpaceAfterFlags = false,
    )
    node.attributes.remove("transform")
    return VisitState.Continue
}

private fun resolveInheritedAttribute(
    node: XastElement,
    parentNode: XastParent?,
    name: String,
): String? {
    node.attributes[name]?.let { return it }
    var current = parentNode
    while (current is XastElement) {
        current.attributes[name]?.let { return it }
        break
    }
    return null
}

private fun scaleNumericValues(value: String, scale: Double): String {
    return REG_NUMERIC_VALUES.replace(value) { match ->
        removeLeadingZeroForTransform(match.value.toDouble() * scale)
    }
}

private fun formatNumber(value: Double): String {
    if (value % 1.0 == 0.0 && !value.toString().contains('E', ignoreCase = true)) {
        return value.toLong().toString()
    }
    return value.toString()
}

private fun removeLeadingZeroForTransform(value: Double): String {
    val str = formatNumber(value)
    if (value > 0.0 && value < 1.0 && str.startsWith("0")) {
        return str.substring(startIndex = 1)
    }
    if (value > -1.0 && value < 0.0 && str.length > 1 && str[1] == '0') {
        return str[0].toString() + str.substring(startIndex = 2)
    }
    return str
}

private fun transformAbsolutePoint(matrix: DoubleArray, x: Double, y: Double): Pair<Double, Double> {
    return (matrix[0] * x + matrix[2] * y + matrix[4]) to (matrix[1] * x + matrix[3] * y + matrix[5])
}

private fun transformRelativePoint(matrix: DoubleArray, x: Double, y: Double): Pair<Double, Double> {
    return (matrix[0] * x + matrix[2] * y) to (matrix[1] * x + matrix[3] * y)
}

@Suppress("CyclomaticComplexity", "LongMethod")
private fun applyMatrixToPathData(pathData: List<PathDataItem>, matrix: DoubleArray) {
    val start = doubleArrayOf(0.0, 0.0)
    val cursor = doubleArrayOf(0.0, 0.0)
    for (pathItem in pathData) {
        var command = pathItem.command
        var args = pathItem.args
        if (command == 'M') {
            cursor[0] = args[0]
            cursor[1] = args[1]
            start[0] = cursor[0]
            start[1] = cursor[1]
            val (x, y) = transformAbsolutePoint(matrix, args[0], args[1])
            args[0] = x
            args[1] = y
        }
        if (command == 'm') {
            cursor[0] += args[0]
            cursor[1] += args[1]
            start[0] = cursor[0]
            start[1] = cursor[1]
            val (x, y) = transformRelativePoint(matrix, args[0], args[1])
            args[0] = x
            args[1] = y
        }
        if (command == 'H') {
            command = 'L'
            args = mutableListOf(args[0], cursor[1])
        }
        if (command == 'h') {
            command = 'l'
            args = mutableListOf(args[0], 0.0)
        }
        if (command == 'V') {
            command = 'L'
            args = mutableListOf(cursor[0], args[0])
        }
        if (command == 'v') {
            command = 'l'
            args = mutableListOf(0.0, args[0])
        }
        if (command == 'L') {
            cursor[0] = args[0]
            cursor[1] = args[1]
            val (x, y) = transformAbsolutePoint(matrix, args[0], args[1])
            args[0] = x
            args[1] = y
        }
        if (command == 'l') {
            cursor[0] += args[0]
            cursor[1] += args[1]
            val (x, y) = transformRelativePoint(matrix, args[0], args[1])
            args[0] = x
            args[1] = y
        }
        if (command == 'C') {
            cursor[0] = args[4]
            cursor[1] = args[5]
            val (x1, y1) = transformAbsolutePoint(matrix, args[0], args[1])
            val (x2, y2) = transformAbsolutePoint(matrix, args[2], args[3])
            val (x, y) = transformAbsolutePoint(matrix, args[4], args[5])
            args[0] = x1
            args[1] = y1
            args[2] = x2
            args[3] = y2
            args[4] = x
            args[5] = y
        }
        if (command == 'c') {
            cursor[0] += args[4]
            cursor[1] += args[5]
            val (x1, y1) = transformRelativePoint(matrix, args[0], args[1])
            val (x2, y2) = transformRelativePoint(matrix, args[2], args[3])
            val (x, y) = transformRelativePoint(matrix, args[4], args[5])
            args[0] = x1
            args[1] = y1
            args[2] = x2
            args[3] = y2
            args[4] = x
            args[5] = y
        }
        if (command == 'S') {
            cursor[0] = args[2]
            cursor[1] = args[3]
            val (x2, y2) = transformAbsolutePoint(matrix, args[0], args[1])
            val (x, y) = transformAbsolutePoint(matrix, args[2], args[3])
            args[0] = x2
            args[1] = y2
            args[2] = x
            args[3] = y
        }
        if (command == 's') {
            cursor[0] += args[2]
            cursor[1] += args[3]
            val (x2, y2) = transformRelativePoint(matrix, args[0], args[1])
            val (x, y) = transformRelativePoint(matrix, args[2], args[3])
            args[0] = x2
            args[1] = y2
            args[2] = x
            args[3] = y
        }
        if (command == 'Q') {
            cursor[0] = args[2]
            cursor[1] = args[3]
            val (x1, y1) = transformAbsolutePoint(matrix, args[0], args[1])
            val (x, y) = transformAbsolutePoint(matrix, args[2], args[3])
            args[0] = x1
            args[1] = y1
            args[2] = x
            args[3] = y
        }
        if (command == 'q') {
            cursor[0] += args[2]
            cursor[1] += args[3]
            val (x1, y1) = transformRelativePoint(matrix, args[0], args[1])
            val (x, y) = transformRelativePoint(matrix, args[2], args[3])
            args[0] = x1
            args[1] = y1
            args[2] = x
            args[3] = y
        }
        if (command == 'T') {
            cursor[0] = args[0]
            cursor[1] = args[1]
            val (x, y) = transformAbsolutePoint(matrix, args[0], args[1])
            args[0] = x
            args[1] = y
        }
        if (command == 't') {
            cursor[0] += args[0]
            cursor[1] += args[1]
            val (x, y) = transformRelativePoint(matrix, args[0], args[1])
            args[0] = x
            args[1] = y
        }
        if (command == 'A') {
            transformArc(cursor = cursor, arc = args, transform = matrix)
            cursor[0] = args[ARC_X_INDEX]
            cursor[1] = args[ARC_Y_INDEX]
            if (abs(args[ARC_ROTATION_INDEX]) > ROTATION_THRESHOLD) {
                val a = args[ARC_RX_INDEX]
                val rotation = args[ARC_ROTATION_INDEX]
                args[ARC_RX_INDEX] = args[ARC_RY_INDEX]
                args[ARC_RY_INDEX] = a
                args[ARC_ROTATION_INDEX] = rotation + if (rotation > 0) -90.0 else 90.0
            }
            val (x, y) = transformAbsolutePoint(matrix, args[ARC_X_INDEX], args[ARC_Y_INDEX])
            args[ARC_X_INDEX] = x
            args[ARC_Y_INDEX] = y
        }
        if (command == 'a') {
            transformArc(cursor = doubleArrayOf(0.0, 0.0), arc = args, transform = matrix)
            cursor[0] += args[ARC_X_INDEX]
            cursor[1] += args[ARC_Y_INDEX]
            if (abs(args[ARC_ROTATION_INDEX]) > ROTATION_THRESHOLD) {
                val a = args[ARC_RX_INDEX]
                val rotation = args[ARC_ROTATION_INDEX]
                args[ARC_RX_INDEX] = args[ARC_RY_INDEX]
                args[ARC_RY_INDEX] = a
                args[ARC_ROTATION_INDEX] = rotation + if (rotation > 0) -90.0 else 90.0
            }
            val (x, y) = transformRelativePoint(matrix, args[ARC_X_INDEX], args[ARC_Y_INDEX])
            args[ARC_X_INDEX] = x
            args[ARC_Y_INDEX] = y
        }
        if (command == 'z' || command == 'Z') {
            cursor[0] = start[0]
            cursor[1] = start[1]
        }
        pathItem.command = command
        val newArgs = args.toList()
        pathItem.args.clear()
        pathItem.args.addAll(newArgs)
    }
}

@Suppress("MagicNumber", "CyclomaticComplexity")
private fun transformArc(cursor: DoubleArray, arc: MutableList<Double>, transform: DoubleArray) {
    val x = arc[ARC_X_INDEX] - cursor[0]
    val y = arc[ARC_Y_INDEX] - cursor[1]
    var a = arc[ARC_RX_INDEX]
    var b = arc[ARC_RY_INDEX]
    val rot = arc[ARC_ROTATION_INDEX] * PI / 180.0
    val cosRot = cos(rot)
    val sinRot = sin(rot)
    if (a > 0 && b > 0) {
        var h = (x * cosRot + y * sinRot).pow(2) / (4.0 * a * a) +
            (y * cosRot - x * sinRot).pow(2) / (4.0 * b * b)
        if (h > 1) {
            h = sqrt(h)
            a *= h
            b *= h
        }
    }
    val ellipse = doubleArrayOf(a * cosRot, a * sinRot, -b * sinRot, b * cosRot, 0.0, 0.0)
    val m = multiplyTransformMatrices(a = transform, b = ellipse)
    val lastCol = m[2] * m[2] + m[3] * m[3]
    val squareSum = m[0] * m[0] + m[1] * m[1] + lastCol
    val root = hypot(m[0] - m[3], m[1] + m[2]) * hypot(m[0] + m[3], m[1] - m[2])
    if (root == 0.0) {
        arc[ARC_RX_INDEX] = sqrt(squareSum / 2.0)
        arc[ARC_RY_INDEX] = arc[ARC_RX_INDEX]
        arc[ARC_ROTATION_INDEX] = 0.0
    } else {
        val majorAxisSqr = (squareSum + root) / 2.0
        val minorAxisSqr = (squareSum - root) / 2.0
        val major = abs(majorAxisSqr - lastCol) > DECOMPOSE_EPSILON
        val sub = (if (major) majorAxisSqr else minorAxisSqr) - lastCol
        val rowsSum = m[0] * m[2] + m[1] * m[3]
        val term1 = m[0] * sub + m[2] * rowsSum
        val term2 = m[1] * sub + m[3] * rowsSum
        arc[ARC_RX_INDEX] = sqrt(majorAxisSqr)
        arc[ARC_RY_INDEX] = sqrt(minorAxisSqr)
        val sign = if (major) { if (term2 < 0) -1.0 else 1.0 } else { if (term1 > 0) -1.0 else 1.0 }
        arc[ARC_ROTATION_INDEX] = sign * acos((if (major) term1 else term2) / hypot(term1, term2)) * 180.0 / PI
    }
    if ((transform[0] < 0) != (transform[3] < 0)) {
        arc[ARC_SWEEP_INDEX] = 1.0 - arc[ARC_SWEEP_INDEX]
    }
}

data class ApplyTransformsParams(
    val transformPrecision: Int,
    val applyTransformsStroked: Boolean,
) : PluginParams,
    Map<String, Any> by mapOf(
        "transformPrecision" to transformPrecision,
        "applyTransformsStroked" to applyTransformsStroked,
    )
