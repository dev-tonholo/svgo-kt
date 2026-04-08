package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.path.PathDataItem
import svgokt.path.js2path
import svgokt.path.path2js
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

private const val DEFAULT_FLOAT_PRECISION = 3
private const val MOVETO_ARGS = 2
private const val ARC_ARGS = 7
private const val ARC_LARGE_ARC_INDEX = 3
private const val ARC_SWEEP_INDEX = 4

/** Index of the end-X coordinate in a cubic bezier (C) command. */
private const val CUBIC_END_X_INDEX = 4

/** Index of the end-Y coordinate in a cubic bezier (C) command. */
private const val CUBIC_END_Y_INDEX = 5

/** Index of the end-X coordinate in a smooth/quadratic (S/Q) command. */
private const val SQ_END_X_INDEX = 2

/** Index of the end-Y coordinate in a smooth/quadratic (S/Q) command. */
private const val SQ_END_Y_INDEX = 3

/** Index of the end-X coordinate in an arc (A) command. */
private const val ARC_END_X_INDEX = 5

/** Index of the end-Y coordinate in an arc (A) command. */
private const val ARC_END_Y_INDEX = 6

/** Absolute tolerance for comparing floating-point coordinates. */
private const val EPSILON = 1e-9

data class MakeArcs(
    val threshold: Float,
    val tolerance: Float,
)

data class ConvertPathDataParams(
    val applyTransforms: Boolean = true,
    val applyTransformsStroked: Boolean = true,
    val makeArcs: MakeArcs = MakeArcs(
        threshold = 2.5f,
        tolerance = 0.5f,
    ),
    val straightCurves: Boolean = true,
    val convertToQ: Boolean = true,
    val lineShorthands: Boolean = true,
    val convertToZ: Boolean = true,
    val curveSmoothShorthands: Boolean = true,
    val floatPrecision: Int = DEFAULT_FLOAT_PRECISION,
    val transformPrecision: Int = 5,
    val smartArcRounding: Boolean = true,
    val removeUseless: Boolean = true,
    val collapseRepeated: Boolean = true,
    val utilizeAbsolute: Boolean = true,
    val leadingZero: Boolean = true,
    val negativeExtraSpace: Boolean = true,
    val noSpaceAfterFlags: Boolean = false,
    val forceAbsolutePath: Boolean = false,
) : PluginParams,
    Map<String, Any> by mapOf(
        "applyTransforms" to applyTransforms,
        "applyTransformsStroked" to applyTransformsStroked,
        "makeArcs" to makeArcs,
        "straightCurves" to straightCurves,
        "convertToQ" to convertToQ,
        "lineShorthands" to lineShorthands,
        "convertToZ" to convertToZ,
        "curveSmoothShorthands" to curveSmoothShorthands,
        "floatPrecision" to floatPrecision,
        "transformPrecision" to transformPrecision,
        "smartArcRounding" to smartArcRounding,
        "removeUseless" to removeUseless,
        "collapseRepeated" to collapseRepeated,
        "utilizeAbsolute" to utilizeAbsolute,
        "leadingZero" to leadingZero,
        "negativeExtraSpace" to negativeExtraSpace,
        "noSpaceAfterFlags" to noSpaceAfterFlags,
        "forceAbsolutePath" to forceAbsolutePath,
    )

/**
 * Path elements that have a 'd' attribute to optimize.
 */
private val PATH_ELEMS = setOf("path", "glyph", "missing-glyph")

class ConvertPathData(
    override val params: ConvertPathDataParams = ConvertPathDataParams(),
) : Plugin<ConvertPathDataParams> {
    override val name: String = "convertPathData"
    override val description: String = "optimizes path data: writes in shorter form, applies transformations"

    /**
     * Convert absolute Path to relative,
     * collapse repeated instructions,
     * detect and convert Lineto shorthands,
     * remove useless instructions like "l0,0",
     * trim useless delimiters and leading zeros,
     * decrease accuracy of floating-point numbers.
     *
     * @see https://www.w3.org/TR/SVG11/paths.html#PathData
     *
     * @author Kir Belevich / parsed to kotlin by Rafael Tonholo
     */
    override val fn: PluginFn = { _, pluginParams, _ ->
        val resolvedParams = pluginParams as? ConvertPathDataParams ?: params

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    processElement(node = node, effectiveParams = resolvedParams)
                },
            ),
        )
    }

    companion object {
        @Suppress("ReturnCount")
        private fun processElement(
            node: XastElement,
            effectiveParams: ConvertPathDataParams,
        ): VisitState {
            if (node.name !in PATH_ELEMS) {
                return VisitState.Continue
            }

            val originalD = node.attributes["d"]
            if (originalD.isNullOrBlank()) {
                return VisitState.Continue
            }

            val pathData = path2js(element = node)
            if (pathData.isEmpty()) {
                return VisitState.Continue
            }

            val precision = effectiveParams.floatPrecision
            val optimized = optimizePathData(
                pathData = pathData,
                params = effectiveParams,
                precision = precision,
            )

            js2path(
                element = node,
                data = optimized,
                precision = precision,
                noSpaceAfterFlags = effectiveParams.noSpaceAfterFlags,
            )

            // Revert if optimized version is longer
            val newD = node.attributes["d"]
            if (newD != null && newD.length > originalD.length) {
                node.attributes["d"] = originalD
            }

            return VisitState.Continue
        }

        @Suppress("CyclomaticComplexity", "CyclomaticComplexMethod", "NestedBlockDepth", "LongMethod")
        private fun optimizePathData(
            pathData: List<PathDataItem>,
            params: ConvertPathDataParams,
            precision: Int,
        ): List<PathDataItem> {
            val result = mutableListOf<PathDataItem>()

            // Track current absolute position
            var curX = 0.0
            var curY = 0.0
            var startX = 0.0
            var startY = 0.0

            for (item in pathData) {
                val command = item.command
                val args = item.args.toMutableList()

                // Round data
                roundArgs(args = args, precision = precision, command = command)

                when (command.uppercaseChar()) {
                    'M' -> {
                        if (command == 'M') {
                            curX = args[0]
                            curY = args[1]
                        } else {
                            curX += args[0]
                            curY += args[1]
                        }
                        startX = curX
                        startY = curY
                        result.add(PathDataItem(command = command, args = args))
                    }

                    'L' -> {
                        val newItem = optimizeLine(
                            command = command,
                            args = args,
                            curX = curX,
                            curY = curY,
                            params = params,
                        )
                        updatePosition(command = command, args = args, curX = curX, curY = curY).let {
                            curX = it.first
                            curY = it.second
                        }
                        result.add(newItem)
                    }

                    'H' -> {
                        if (command == 'H') {
                            curX = args[0]
                        } else {
                            curX += args[0]
                        }
                        result.add(PathDataItem(command = command, args = args))
                    }

                    'V' -> {
                        if (command == 'V') {
                            curY = args[0]
                        } else {
                            curY += args[0]
                        }
                        result.add(PathDataItem(command = command, args = args))
                    }

                    'Z' -> {
                        curX = startX
                        curY = startY
                        result.add(PathDataItem(command = command, args = args))
                    }

                    'C', 'S', 'Q', 'T' -> {
                        updatePosition(command = command, args = args, curX = curX, curY = curY).let {
                            curX = it.first
                            curY = it.second
                        }
                        result.add(PathDataItem(command = command, args = args))
                    }

                    'A' -> {
                        updatePosition(command = command, args = args, curX = curX, curY = curY).let {
                            curX = it.first
                            curY = it.second
                        }
                        result.add(PathDataItem(command = command, args = args))
                    }

                    else -> {
                        result.add(PathDataItem(command = command, args = args))
                    }
                }
            }

            // Remove useless commands
            if (params.removeUseless) {
                return removeUselessCommands(result)
            }

            return result
        }

        private fun roundArgs(args: MutableList<Double>, precision: Int, command: Char) {
            val pow = 10.0.pow(precision)
            for (i in args.indices) {
                // Don't round arc flags
                if ((command == 'A' || command == 'a') &&
                    (i % ARC_ARGS == ARC_LARGE_ARC_INDEX || i % ARC_ARGS == ARC_SWEEP_INDEX)
                ) {
                    continue
                }
                args[i] = round(args[i] * pow) / pow
            }
        }

        @Suppress("ReturnCount")
        private fun optimizeLine(
            command: Char,
            args: MutableList<Double>,
            curX: Double,
            curY: Double,
            params: ConvertPathDataParams,
        ): PathDataItem {
            if (!params.lineShorthands) {
                return PathDataItem(command = command, args = args)
            }

            val isAbsolute = command == 'L'

            val dx = if (isAbsolute) args[0] - curX else args[0]
            val dy = if (isAbsolute) args[1] - curY else args[1]

            // Convert to H (horizontal line) when dy == 0
            if (abs(dy) < EPSILON) {
                val hArg = if (isAbsolute) args[0] else dx
                return PathDataItem(
                    command = if (isAbsolute) 'H' else 'h',
                    args = mutableListOf(hArg),
                )
            }

            // Convert to V (vertical line) when dx == 0
            if (abs(dx) < EPSILON) {
                val vArg = if (isAbsolute) args[1] else dy
                return PathDataItem(
                    command = if (isAbsolute) 'V' else 'v',
                    args = mutableListOf(vArg),
                )
            }

            return PathDataItem(command = command, args = args)
        }

        @Suppress("CyclomaticComplexMethod")
        private fun updatePosition(
            command: Char,
            args: MutableList<Double>,
            curX: Double,
            curY: Double,
        ): Pair<Double, Double> {
            if (args.isEmpty()) return curX to curY

            val isAbsolute = command.isUpperCase()
            return when (command.uppercaseChar()) {
                'M', 'L', 'T' -> {
                    if (isAbsolute) {
                        args[0] to args[1]
                    } else {
                        (curX + args[0]) to (curY + args[1])
                    }
                }

                'H' -> {
                    if (isAbsolute) {
                        args[0] to curY
                    } else {
                        (curX + args[0]) to curY
                    }
                }

                'V' -> {
                    if (isAbsolute) {
                        curX to args[0]
                    } else {
                        curX to (curY + args[0])
                    }
                }

                'C' -> {
                    if (isAbsolute) {
                        args[CUBIC_END_X_INDEX] to args[CUBIC_END_Y_INDEX]
                    } else {
                        (curX + args[CUBIC_END_X_INDEX]) to (curY + args[CUBIC_END_Y_INDEX])
                    }
                }

                'S', 'Q' -> {
                    if (isAbsolute) {
                        args[SQ_END_X_INDEX] to args[SQ_END_Y_INDEX]
                    } else {
                        (curX + args[SQ_END_X_INDEX]) to (curY + args[SQ_END_Y_INDEX])
                    }
                }

                'A' -> {
                    if (isAbsolute) {
                        args[ARC_END_X_INDEX] to args[ARC_END_Y_INDEX]
                    } else {
                        (curX + args[ARC_END_X_INDEX]) to (curY + args[ARC_END_Y_INDEX])
                    }
                }

                else -> curX to curY
            }
        }

        /**
         * Remove useless path commands:
         * - l0,0 / L(curX,curY) - line to current position
         * - h0 / H(curX) - horizontal line to current position
         * - v0 / V(curY) - vertical line to current position
         */
        private fun removeUselessCommands(pathData: List<PathDataItem>): List<PathDataItem> {
            return pathData.filter { item ->
                val cmd = item.command
                val args = item.args

                when {
                    // Keep all non-line commands
                    cmd.uppercaseChar() !in setOf('L', 'H', 'V') -> true
                    // Remove relative zero-movement
                    cmd == 'l' && args.size == MOVETO_ARGS &&
                        abs(args[0]) < EPSILON && abs(args[1]) < EPSILON -> false
                    cmd == 'h' && args.size == 1 && abs(args[0]) < EPSILON -> false
                    cmd == 'v' && args.size == 1 && abs(args[0]) < EPSILON -> false
                    else -> true
                }
            }
        }
    }
}
